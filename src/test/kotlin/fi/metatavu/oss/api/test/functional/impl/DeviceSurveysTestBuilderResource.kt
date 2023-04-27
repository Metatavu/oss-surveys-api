package fi.metatavu.oss.api.test.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.oss.test.client.models.DeviceSurvey
import fi.metatavu.oss.api.test.functional.TestBuilder
import fi.metatavu.oss.api.test.functional.settings.ApiTestSettings
import fi.metatavu.oss.test.client.apis.DeviceSurveysApi
import fi.metatavu.oss.test.client.infrastructure.ApiClient
import fi.metatavu.oss.test.client.infrastructure.ClientException
import fi.metatavu.oss.test.client.models.DeviceApprovalStatus
import org.junit.jupiter.api.fail
import java.util.*

/**
 * Test resources fo device surveys
 */
class DeviceSurveysTestBuilderResource(
    private val testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
): ApiTestBuilderResource<DeviceSurvey, ApiClient>(testBuilder, apiClient) {

    private var deviceKey: String? = null

    override fun clean(t: DeviceSurvey?) {
        api.deleteDeviceSurvey(
            deviceId =  t!!.deviceId,
            deviceSurveyId = t.id!!
        )
    }

    override fun getApi(): DeviceSurveysApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        if (!deviceKey.isNullOrBlank()) {
            ApiClient.apiKey["X-DEVICE-KEY"] = deviceKey!!
        }
        return DeviceSurveysApi(ApiTestSettings.apiBasePath)
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
     * Lists device surveys
     *
     * @param deviceId device id
     * @param firstResult first result
     * @param maxResults max results
     * @return list of device surveys
     */
    fun list(
        deviceId: UUID,
        firstResult: Int?,
        maxResults: Int?
    ): Array<DeviceSurvey> {
        return api.listDeviceSurveys(
            deviceId = deviceId,
            firstResult = firstResult,
            maxResults = maxResults,
        )
    }

    /**
     * Creates a device survey
     *
     * @param deviceId device id
     * @param deviceSurvey device survey to create
     * @return created device survey
     */
    fun create(deviceId: UUID, deviceSurvey: DeviceSurvey): DeviceSurvey {
        return addClosable(
            api.createDeviceSurvey(
                deviceId = deviceId,
                deviceSurvey =  deviceSurvey
            )
        )
    }


    /**
     * Finds a device survey
     *
     * @param deviceId device id
     * @param deviceSurveyId device survey id
     * @return found device survey
     */
    fun find(deviceId: UUID, deviceSurveyId: UUID): DeviceSurvey {
        return api.findDeviceSurvey(
            deviceId = deviceId,
            deviceSurveyId = deviceSurveyId
        )
    }

    /**
     * Updates a device survey
     *
     * @param deviceId device id
     * @param deviceSurveyId device survey id
     * @param deviceSurvey device survey to update
     * @return updated device survey
     */
    fun update(deviceId: UUID, deviceSurveyId: UUID, deviceSurvey: DeviceSurvey): DeviceSurvey {
        return api.updateDeviceSurvey(
            deviceId = deviceId,
            deviceSurveyId = deviceSurveyId,
            deviceSurvey = deviceSurvey
        )
    }

    /**
     * Deletes a device survey
     *
     * @param deviceId device id
     * @param deviceSurveyId device survey id
     */
    fun delete(deviceId: UUID, deviceSurveyId: UUID) {
        api.deleteDeviceSurvey(
            deviceId = deviceId,
            deviceSurveyId = deviceSurveyId
        )

        removeCloseable { closable ->
            if (closable !is DeviceSurvey) {
                return@removeCloseable false
            }

            closable.id == deviceSurveyId
        }
    }

    /**
     * Asserts that listing device surveys fails with given status code
     *
     * @param expectedStatusCode expected status code
     * @param deviceId device id
     */
    fun assertListFail(expectedStatusCode: Int, deviceId: UUID) {
        try {
            api.listDeviceSurveys(deviceId = deviceId)
            fail("Listing device surveys should have failed")
        } catch (e: ClientException) {
            assertClientExceptionStatus(expectedStatusCode, e)
        }
    }

    /**
     * Asserts that creating a device survey fails with given status code
     *
     * @param expectedStatusCode expected status code
     * @param deviceId device id
     * @param deviceSurvey device survey
     */
    fun assertCreateFail(
        expectedStatusCode: Int,
        deviceId: UUID,
        deviceSurvey: DeviceSurvey
    ) {
        try {
            api.createDeviceSurvey(
                deviceId = deviceId,
                deviceSurvey = deviceSurvey
            )
            fail("Creating device survey should have failed")
        } catch (e: ClientException) {
            assertClientExceptionStatus(expectedStatusCode, e)
        }
    }

    /**
     * Asserts that updating a device survey fails with given status code
     *
     * @param expectedStatusCode expected status code
     * @param deviceId device id
     * @param deviceSurveyId device survey id
     * @param deviceSurvey device survey
     */
    fun assertUpdateFail(
        expectedStatusCode: Int,
        deviceId: UUID,
        deviceSurveyId: UUID,
        deviceSurvey: DeviceSurvey
    ) {
        try {
            api.updateDeviceSurvey(
                deviceId = deviceId,
                deviceSurveyId = deviceSurveyId,
                deviceSurvey = deviceSurvey
            )
            fail("Updating device survey should have failed")
        } catch (e: ClientException) {
            assertClientExceptionStatus(expectedStatusCode, e)
        }
    }

    /**
     * Asserts that deleting a device survey fails with given status code
     *
     * @param expectedStatusCode expected status code
     * @param deviceId device id
     * @param deviceSurveyId device survey id
     */
    fun assertDeleteFail(
        expectedStatusCode: Int,
        deviceId: UUID,
        deviceSurveyId: UUID
    ) {
        try {
            api.deleteDeviceSurvey(
                deviceId = deviceId,
                deviceSurveyId = deviceSurveyId
            )
            fail("Deleting device survey should have failed")
        } catch (e: ClientException) {
            assertClientExceptionStatus(expectedStatusCode, e)
        }
    }

    /**
     * Asserts that finding a device survey fails with given status code
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
            api.findDeviceSurvey(
                deviceId = deviceId,
                deviceSurveyId = deviceSurveyId
            )
            fail("Finding device survey should have failed")
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