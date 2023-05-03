package fi.metatavu.oss.api.test.functional.tests

import fi.metatavu.oss.api.test.functional.resources.LocalTestProfile
import fi.metatavu.oss.test.client.models.DeviceApprovalStatus
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.*

/**
 * Tests for Device Requests API
 */
@QuarkusTest
@TestProfile(LocalTestProfile::class)
class DeviceRequestsTest: AbstractResourceTest() {

    @Test
    fun testCreateDeviceRequest() {
        createTestBuilder().use { testBuilder ->
            val deviceRequest = testBuilder.manager.deviceRequests.create("123")

            assertEquals(deviceRequest.serialNumber, "123")
            assertNotNull(deviceRequest.id)
            assertNotNull(deviceRequest.metadata!!.createdAt)
            assertNotNull(deviceRequest.metadata.modifiedAt)

            // Test that cannot create device requests with duplicate serial number
            testBuilder.manager.deviceRequests.assertCreateFail(
                serialNumber = "123",
                expectedStatusCode = 400
            )
        }
    }

    @Test
    fun testCreateDeviceBadRequest() {
        createTestBuilder().use { testBuilder ->
            val deviceRequest = testBuilder.manager.deviceRequests.create("123")

            testBuilder.manager.deviceRequests.assertCreateFail(
                serialNumber = deviceRequest.serialNumber!!,
                expectedStatusCode = 400
            )
        }
    }

    @Test
    fun testUpdateDeviceRequest() {
        createTestBuilder().use { testBuilder ->
            val deviceRequest = testBuilder.manager.deviceRequests.create("123")

            // Test that device key cannot be retrieved before status is approved
            testBuilder.manager.deviceRequests.assertGetKeyFail(
                requestId = deviceRequest.id!!,
                expectedStatusCode = 403
            )

            val updatedDeviceRequest = testBuilder.manager.deviceRequests.updateDeviceRequest(
                requestId = deviceRequest.id,
                deviceRequest = deviceRequest.copy(approvalStatus = DeviceApprovalStatus.APPROVED)
            )

            assertEquals(deviceRequest.id, updatedDeviceRequest.id)
            assertEquals(deviceRequest.serialNumber, updatedDeviceRequest.serialNumber)
            assertEquals(OffsetDateTime.parse(deviceRequest.metadata!!.createdAt).toEpochSecond(), OffsetDateTime.parse(updatedDeviceRequest.metadata!!.createdAt).toEpochSecond())
            assertEquals(updatedDeviceRequest.approvalStatus, DeviceApprovalStatus.APPROVED)

            // Test that can update only as manager
            testBuilder.empty.deviceRequests.assertUpdateFail(
                requestId = UUID.randomUUID(),
                expectedStatusCode = 401
            )
            testBuilder.consumer.deviceRequests.assertUpdateFail(
                requestId = UUID.randomUUID(),
                expectedStatusCode = 403
            )
            testBuilder.notvalid.deviceRequests.assertUpdateFail(
                requestId = UUID.randomUUID(),
                expectedStatusCode = 401
            )
        }
    }

    @Test
    fun testGetDeviceKey() {
        createTestBuilder().use { testBuilder ->
            val deviceRequest = testBuilder.manager.deviceRequests.create("123")

            // Test that device key cannot be retrieved before status is approved
            testBuilder.manager.deviceRequests.assertGetKeyFail(
                requestId = deviceRequest.id!!,
                expectedStatusCode = 403
            )

            testBuilder.manager.deviceRequests.updateDeviceRequest(
                requestId = deviceRequest.id,
                deviceRequest = deviceRequest.copy(approvalStatus = DeviceApprovalStatus.APPROVED)
            )

            val deviceKey = testBuilder.manager.deviceRequests.getDeviceKey(requestId = deviceRequest.id)

            assertNotNull(deviceKey)

            // Test that device request is deleted after key is retrieved (e.g. key can only be retrieved once)
            testBuilder.manager.deviceRequests.assertGetKeyFail(
                requestId = deviceRequest.id,
                expectedStatusCode = 404
            )
        }
    }

    @Test
    fun testDeleteDeviceRequest() {
        createTestBuilder().use { testBuilder ->
            val deviceRequest = testBuilder.manager.deviceRequests.create("123")

            // Test that device requests can only be deleted as a manager
            testBuilder.consumer.deviceRequests.assertDeleteFail(
                requestId = deviceRequest.id!!,
                expectedStatusCode = 403
            )
            testBuilder.empty.deviceRequests.assertDeleteFail(
                requestId = deviceRequest.id,
                expectedStatusCode = 401
            )
            testBuilder.notvalid.deviceRequests.assertDeleteFail(
                requestId = deviceRequest.id,
                expectedStatusCode = 401
            )

            testBuilder.manager.deviceRequests.delete(deviceRequestId = deviceRequest.id)

            testBuilder.manager.deviceRequests.assertGetKeyFail(
                requestId = deviceRequest.id,
                expectedStatusCode = 404
            )
        }
    }
}