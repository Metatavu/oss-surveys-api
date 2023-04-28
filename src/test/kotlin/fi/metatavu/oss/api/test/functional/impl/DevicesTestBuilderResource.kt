package fi.metatavu.oss.api.test.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.oss.api.test.functional.TestBuilder
import fi.metatavu.oss.api.test.functional.settings.ApiTestSettings
import fi.metatavu.oss.test.client.apis.DevicesApi
import fi.metatavu.oss.test.client.infrastructure.ApiClient
import fi.metatavu.oss.test.client.infrastructure.ClientException
import fi.metatavu.oss.test.client.models.Device
import java.util.*

/**
 * Test resources for devices
 */
class DevicesTestBuilderResource(
    testBuilder: TestBuilder,
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
     * @return found devices
     */
    fun list(): Array<Device> {
        return api.listDevices()
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
}