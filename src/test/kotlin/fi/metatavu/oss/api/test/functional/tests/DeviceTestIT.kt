package fi.metatavu.oss.api.test.functional.tests

import fi.metatavu.oss.api.test.functional.mqtt.TestMqttClient
import fi.metatavu.oss.api.test.functional.resources.LocalTestProfile
import fi.metatavu.oss.test.client.models.DeviceStatus
import fi.metatavu.oss.test.client.models.DeviceStatusMessage
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import org.awaitility.Awaitility
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * Tests for device API
 */
@QuarkusTest
@TestProfile(LocalTestProfile::class)
class DeviceTestIT: AbstractResourceTest() {

    @Test
    fun testFindDevices() = createTestBuilder().use { testBuilder ->
        val (deviceId) = testBuilder.manager.devices.setupTestDevice()

        val foundDevice = testBuilder.manager.devices.find(deviceId)

        assertNotNull(foundDevice)
        assertEquals(deviceId, foundDevice.id)
    }

    @Test
    fun testListDevices() = createTestBuilder().use { testBuilder ->
        (1..3).map {
            testBuilder.manager.devices.setupTestDevice(it.toString())
        }

        val foundDevices = testBuilder.manager.devices.list()

        assertEquals(3, foundDevices.size)
        foundDevices.forEach { device ->
            val foundDevice = testBuilder.manager.devices.find(device.id!!)
            assertTrue(foundDevices.any { device == foundDevice })
        }
    }

    @Test
    fun testDeleteDevice() = createTestBuilder().use { testBuilder ->
        val (deviceId) = testBuilder.manager.devices.setupTestDevice()
        val foundDevice = testBuilder.manager.devices.find(deviceId)

        assertNotNull(foundDevice)
        testBuilder.manager.devices.delete(deviceId)

        testBuilder.manager.devices.assertFindFail(404, deviceId)
    }

    @Test
    fun testDeviceStatusMessage() = createTestBuilder().use { testBuilder ->
        val mqttClient = TestMqttClient()
        val (deviceId) = testBuilder.manager.devices.setupTestDevice()
        val foundDevice = testBuilder.manager.devices.find(deviceId)

        assertEquals(DeviceStatus.OFFLINE, foundDevice.deviceStatus)

        mqttClient.publish(
            topic = "$deviceId/status",
            message = DeviceStatusMessage(
                deviceId = deviceId,
                status = DeviceStatus.ONLINE
            )
        )

        Awaitility
            .await()
            .atMost(Duration.ofMinutes(1))
            .pollInterval(Duration.ofSeconds(5))
            .until {
                testBuilder.manager.devices.find(deviceId).deviceStatus == DeviceStatus.ONLINE
            }

        val foundDeviceAfterStatusChange = testBuilder.manager.devices.list(status = DeviceStatus.ONLINE)
        assertEquals(1, foundDeviceAfterStatusChange.size)
    }
}