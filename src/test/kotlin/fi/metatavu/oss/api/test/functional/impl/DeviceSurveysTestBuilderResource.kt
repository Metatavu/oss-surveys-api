package fi.metatavu.oss.api.test.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.oss.api.test.functional.TestBuilder
import fi.metatavu.oss.api.test.functional.settings.ApiTestSettings
import fi.metatavu.oss.test.client.apis.DeviceSurveysApi
import fi.metatavu.oss.test.client.infrastructure.ApiClient
import fi.metatavu.oss.test.client.infrastructure.ClientException
import fi.metatavu.oss.test.client.models.DeviceSurvey
import fi.metatavu.oss.test.client.models.DeviceSurveyStatistics
import fi.metatavu.oss.test.client.models.DeviceSurveyStatus
import org.junit.jupiter.api.fail
import java.time.OffsetDateTime
import java.util.*

/**
 * Test resources fo device surveys
 */
class DeviceSurveysTestBuilderResource(
    val testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
): ApiTestBuilderResource<DeviceSurvey, ApiClient>(testBuilder, apiClient) {

    override fun clean(t: DeviceSurvey?) {
        api.deleteDeviceSurvey(
            deviceId =  t!!.deviceId,
            deviceSurveyId = t.id!!
        )
    }

    override fun getApi(): DeviceSurveysApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return DeviceSurveysApi(ApiTestSettings.apiBasePath)
    }

    /**
     * Lists device surveys by device id
     *
     * @param deviceId device id
     * @param firstResult first result
     * @param maxResults max results
     * @param status status
     * @return list of device surveys
     */
    fun list(
        deviceId: UUID,
        firstResult: Int? = null,
        maxResults: Int? = null,
        status: DeviceSurveyStatus? = null
    ): Array<DeviceSurvey> {
        return api.listDeviceSurveys(
            deviceId = deviceId,
            firstResult = firstResult,
            maxResults = maxResults,
            status = status
        )
    }

    /**
     * Lists all device surveys
     *
     * @return list of device surveys
     */
    fun list(): Array<DeviceSurvey> {
        val deviceSurveys = mutableListOf<DeviceSurvey>()
        val devices = testBuilder.manager.devices.list()
        for (device in devices) {
            deviceSurveys.addAll(list(deviceId = device.id!!))
        }
        return deviceSurveys.toTypedArray()
    }

    /**
     * Creates a device survey
     *
     * @param deviceId device id
     * @param deviceSurvey device survey to create
     * @param addClosable add closable, defaults to true
     * @return created device survey
     */
    fun create(deviceId: UUID, deviceSurvey: DeviceSurvey, addClosable: Boolean = true): DeviceSurvey {
        val created = api.createDeviceSurvey(
            deviceId = deviceId,
            deviceSurvey =  deviceSurvey
        )

        if (!addClosable) {
          return created
        }

        return addClosable(created)
    }

    /**
     * Creates valid survey with published status
     *
     * @param deviceId device id
     * @param surveyId survey id
     * @param addClosable add closable
     * @return device survey
     */
    fun createCurrentlyPublishedDeviceSurvey(
        deviceId: UUID,
        surveyId: UUID,
        addClosable: Boolean = true
    ): DeviceSurvey {
        val now = OffsetDateTime.now()
        return create(deviceId = deviceId,
            deviceSurvey = DeviceSurvey(
                surveyId = surveyId,
                deviceId = deviceId,
                status = DeviceSurveyStatus.PUBLISHED,
                publishStartTime = now.toString(),
                publishEndTime = now.plusDays(1).toString()
            ),
            addClosable = addClosable
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
     * Returns device survey statistics
     *
     * @param deviceId device id
     * @param surveyId survey id
     * @return device survey statistics
     */
    fun getDeviceSurveyStatistics(deviceId: UUID, surveyId: UUID): DeviceSurveyStatistics {
        return api.getDeviceSurveyStatistics(
            deviceId = deviceId,
            surveyId = surveyId
        )
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
     * Asserts that getting device survey statistics fails with given status code
     *
     * @param expectedStatusCode expected status code
     * @param deviceId device id
     * @param surveyId survey id
     */
    fun assertGetDeviceSurveyStatisticsFail(expectedStatusCode: Int, deviceId: UUID, surveyId: UUID) {
        try {
            api.getDeviceSurveyStatistics(
                deviceId = deviceId,
                surveyId = surveyId
            )
            fail("Getting device survey statistics should have failed")
        } catch (e: ClientException) {
            assertClientExceptionStatus(expectedStatusCode, e)
        }
    }
}