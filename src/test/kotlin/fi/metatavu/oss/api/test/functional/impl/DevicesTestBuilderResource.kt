package fi.metatavu.oss.api.test.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.oss.api.test.functional.TestBuilder
import fi.metatavu.oss.api.test.functional.settings.ApiTestSettings
import fi.metatavu.oss.test.client.apis.DevicesApi
import fi.metatavu.oss.test.client.infrastructure.ApiClient
import fi.metatavu.oss.test.client.infrastructure.ClientException
import fi.metatavu.oss.test.client.models.Device
import fi.metatavu.oss.test.client.models.DeviceApprovalStatus
import fi.metatavu.oss.test.client.models.DeviceStatus
import java.util.*

/**
 * Test resources for devices
 */
class DevicesTestBuilderResource(
    private val testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
) : ApiTestBuilderResource<Device, ApiClient>(testBuilder, apiClient) {
    override fun clean(t: Device?) {
        api.deleteDevice(t!!.id!!)

    }

    override fun getApi(): DevicesApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return DevicesApi(ApiTestSettings.apiBasePath)
    }

    /**
     * Deletes a device
     *
     * @param deviceId device id
     */
    fun delete(deviceId: UUID) {
        api.deleteDevice(deviceId)
        removeCloseable { closable ->
            if (closable !is Device) {
                return@removeCloseable false
            }

            closable.id!! == deviceId
        }
    }

    /**
     * Lists devices
     *
     * @param status optonal status filter
     * @return found devices
     */
    fun list(status: DeviceStatus? = null): Array<Device> {
        return api.listDevices(status = status)
    }

    /**
     * Finds a device
     *
     * @param id id
     * @return found device
     */
    fun find(id: UUID): Device {
        return api.findDevice(id)
    }

    /**
     * Asserts that finding device fails with given status code
     *
     * @param expectedStatusCode expected status code
     * @param id id
     */
    fun assertFindFail(expectedStatusCode: Int, id: UUID) {
        try {
            api.findDevice(id)
        } catch (e: ClientException) {
            assertClientExceptionStatus(expectedStatusCode, e)
        }
    }

    /**
     * Setups an approved device for use in tests.
     *
     * Devices are created via device registration API and are not automatically approved,
     * therefore this simplifies that flow in tests.
     *
     * @param serialNumber optional serial number
     * @return pair of device id and device key
     */
    fun setupTestDevice(serialNumber: String = "123"): Pair<UUID, String> {
        val deviceRequest = testBuilder.manager.deviceRequests.create(serialNumber)
        testBuilder.manager.deviceRequests.updateDeviceRequest(
            requestId = deviceRequest.id!!,
            deviceRequest = deviceRequest.copy(approvalStatus = DeviceApprovalStatus.APPROVED)
        )
        val deviceKey = testBuilder.manager.deviceRequests.getDeviceKey(deviceRequest.id)

        return Pair(deviceRequest.id, deviceKey)
    }
}