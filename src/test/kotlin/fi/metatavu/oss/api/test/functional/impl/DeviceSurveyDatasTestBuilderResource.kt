package fi.metatavu.oss.api.test.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.oss.api.test.functional.TestBuilder
import fi.metatavu.oss.api.test.functional.settings.ApiTestSettings
import fi.metatavu.oss.test.client.apis.DeviceDataApi
import fi.metatavu.oss.test.client.infrastructure.ApiClient
import fi.metatavu.oss.test.client.infrastructure.ClientException
import fi.metatavu.oss.test.client.models.DeviceApprovalStatus
import fi.metatavu.oss.test.client.models.DeviceSurvey
import fi.metatavu.oss.test.client.models.DeviceSurveyData
import org.junit.jupiter.api.fail
import java.util.*

/**
 * Test resources fo device survey data
 */
class DeviceSurveyDatasTestBuilderResource(
    private val testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
) : ApiTestBuilderResource<DeviceSurvey, ApiClient>(testBuilder, apiClient) {

    private var deviceKey: String? = null
    override fun clean(t: DeviceSurvey?) {
        // read-only resource
    }

    override fun getApi(): DeviceDataApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        if (!deviceKey.isNullOrBlank()) {
            ApiClient.apiKey["X-DEVICE-KEY"] = deviceKey!!
        }
        return DeviceDataApi(ApiTestSettings.apiBasePath)
    }

    /**
     * Sets the clients device key
     *
     * @param newDeviceKey device key
     */
    fun setDeviceKey(newDeviceKey: String?) {
        deviceKey = newDeviceKey
    }

    /**
     * Lists device surveys data
     *
     * @param deviceId device id
     * @return list of device survey data
     */
    fun list(
        deviceId: UUID,
    ): Array<DeviceSurveyData> {
        return api.listDeviceDataSurveys(
            deviceId = deviceId
        )
    }

    /**
     * Finds a device survey data
     *
     * @param deviceId device id
     * @param deviceSurveyId device survey id
     * @return found device survey data
     */
    fun find(deviceId: UUID, deviceSurveyId: UUID): DeviceSurveyData {
        return api.findDeviceDataSurvey(
            deviceId = deviceId,
            deviceSurveyId = deviceSurveyId
        )
    }

    /**
     * Asserts that listing device survey datas fails with given status code
     *
     * @param expectedStatusCode expected status code
     * @param deviceId device id
     */
    fun assertListFail(expectedStatusCode: Int, deviceId: UUID) {
        try {
            api.listDeviceDataSurveys(deviceId = deviceId)
            fail("Listing device surveys data should have failed")
        } catch (e: ClientException) {
            assertClientExceptionStatus(expectedStatusCode, e)
        }
    }

    /**
     * Asserts that finding a device survey data fails with given status code
     *
     * @param expectedStatusCode expected status code
     * @param deviceId device id
     * @param deviceSurveyId device survey id
     */
    fun assertFindFail(
        expectedStatusCode: Int,
        deviceId: UUID,
        deviceSurveyId: UUID
    ) {
        try {
            api.findDeviceDataSurvey(
                deviceId = deviceId,
                deviceSurveyId = deviceSurveyId
            )
            fail("Finding device survey data should have failed")
        } catch (e: ClientException) {
            assertClientExceptionStatus(expectedStatusCode, e)
        }
    }
}