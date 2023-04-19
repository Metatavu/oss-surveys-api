package fi.metatavu.oss.api.test.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.oss.test.client.apis.DeviceRequestsApi
import fi.metatavu.oss.api.test.functional.TestBuilder
import fi.metatavu.oss.api.test.functional.settings.ApiTestSettings
import fi.metatavu.oss.test.client.infrastructure.ApiClient
import fi.metatavu.oss.test.client.infrastructure.ClientException
import fi.metatavu.oss.test.client.models.DeviceRequest
import org.junit.jupiter.api.Assertions
import java.util.*

/**
 * Test resources for device requests
 */
class DeviceRequestsTestBuilderResource(
    testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
) : ApiTestBuilderResource<DeviceRequest, ApiClient>(testBuilder, apiClient) {
    override fun clean(t: DeviceRequest?) {
        api.deleteDeviceRequest(t!!.id!!)
    }

    override fun getApi(): DeviceRequestsApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return DeviceRequestsApi(ApiTestSettings.apiBasePath)
    }

    /**
     * Deletes Device Request
     *
     * @param deviceRequestId device request id
     */
    fun delete(deviceRequestId: UUID) {
        api.deleteDeviceRequest(deviceRequestId)
        remove(deviceRequestId = deviceRequestId)
    }

    /**
     * Removes closable
     *
     * @param deviceRequestId revice request id
     */
    private fun remove(deviceRequestId: UUID) {
        removeCloseable { closeable ->
            if (closeable !is DeviceRequest) {
                return@removeCloseable false
            }

            closeable.id!! == deviceRequestId
        }
    }

    /**
     * Creates Device Request
     *
     * @param serialNumber serial number
     * @return created device request
     */
    fun create(serialNumber: String): DeviceRequest {
        return addClosable(api.createDeviceRequest(serialNumber = serialNumber))
    }

    /**
     * Gets Device Key
     *
     * @param requestId request id
     * @return device key
     */
    fun getDeviceKey(requestId: UUID): String {
        val deviceKey =  api.getDeviceKey(requestId = requestId).key
        remove(deviceRequestId = requestId)

        return deviceKey
    }

    /**
     * Updates Device Request
     *
     * @param requestId request id
     * @param deviceRequest deviceRequest
     * @return updated device request
     */
    fun updateDeviceRequest(requestId: UUID, deviceRequest: DeviceRequest): DeviceRequest {
        return api.updateDeviceRequest(
            requestId = requestId,
            deviceRequest = deviceRequest
        )
    }

    /**
     * Asserts that creating device request fails with given status code
     *
     * @param serialNumber serial number
     * @param expectedStatusCode expected status code
     */
    fun assertCreateFail(serialNumber: String, expectedStatusCode: Int) {
        try {
            api.createDeviceRequest(serialNumber = serialNumber)
            Assertions.fail("Create should have failed")
        } catch (e: ClientException) {
            assertClientExceptionStatus(expectedStatusCode, e)
        }
    }

    /**
     * Asserts that getting device key fails with given status code
     *
     * @param requestId requst id
     * @param expectedStatusCode expected status code
     */
    fun assertGetKeyFail(requestId: UUID, expectedStatusCode: Int) {
        try {
            api.getDeviceKey(requestId = requestId)
            Assertions.fail("Getting device key should have failed")
        } catch (e: ClientException) {
            assertClientExceptionStatus(expectedStatusCode, e)
        }
    }

    /**
     * Asserts that updating device request fails with given status code
     *
     * @param requestId request id
     * @param expectedStatusCode expected status code
     */
    fun assertUpdateFail(requestId: UUID, expectedStatusCode: Int) {
        try {
            api.updateDeviceRequest(
                requestId = requestId,
                deviceRequest = DeviceRequest()
            )
            Assertions.fail("Updating device request should have failed")
        } catch (e: ClientException) {
            assertClientExceptionStatus(expectedStatusCode, e)
        }
    }

    /**
     * Asserts that deleting device request fails with given status code
     *
     * @param requestId request id
     * @param expectedStatusCode expected status code
     */
    fun assertDeleteFail(requestId: UUID, expectedStatusCode: Int) {
        try {
            api.deleteDeviceRequest(requestId = requestId)
            Assertions.fail("Deleting device request should have failed")
        } catch (e: ClientException) {
            assertClientExceptionStatus(expectedStatusCode, e)
        }
    }
}