package fi.metatavu.oss.api.test.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.oss.api.test.functional.TestBuilder
import fi.metatavu.oss.api.test.functional.settings.ApiTestSettings
import fi.metatavu.oss.test.client.apis.DeviceDataApi
import fi.metatavu.oss.test.client.infrastructure.ApiClient
import fi.metatavu.oss.test.client.infrastructure.ClientException
import fi.metatavu.oss.test.client.models.DevicePageSurveyAnswer
import fi.metatavu.oss.test.client.models.DeviceSurvey
import fi.metatavu.oss.test.client.models.DeviceSurveyData
import org.junit.jupiter.api.fail
import java.util.*

/**
 * Test resources fo device survey data
 */
class DeviceSurveyDatasTestBuilderResource(
    testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient,
    private val surveyAnswersResource: SurveyAnswersTestBuilderResource
) : ApiTestBuilderResource<DeviceSurvey, ApiClient>(testBuilder, apiClient) {

    private var deviceKey: String? = null

    // List to help keep track of which device answers were already added as closable resources
    private val submittedClosableAnswersIDs = mutableListOf<UUID>()

    override fun clean(t: DeviceSurvey?) {
        // cleaned in survey answers resource instead
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
     * Submits the answer from the survey using the V2 API
     *
     * @param deviceId device id
     * @param devicePageSurveyAnswer device page survey answer
     * @param surveyId survey id (not needed for actual request)
     * @param pageId page id (not needed for actual request)
     */
    fun submitSurveyAnswer(
        deviceId: UUID,
        devicePageSurveyAnswer: DevicePageSurveyAnswer,
        surveyId: UUID,
        pageId: UUID
    ) {
        api.submitSurveyAnswerV2(
            deviceId = deviceId,
            devicePageSurveyAnswer = devicePageSurveyAnswer
        )

        // add it as closable to resource which can delete the answer
        surveyAnswersResource.list(
            surveyId = surveyId,
            pageId = pageId
        ).forEach {
            if (submittedClosableAnswersIDs.contains(it.id)) {
                return@forEach
            }
            surveyAnswersResource.addClosable(it)
            surveyAnswersResource.answerToSurvey[it.id!!] = surveyId
            submittedClosableAnswersIDs.add(it.id)
        }
    }

    /**
     * Submits the answer from the survey
     *
     * @param deviceId device id
     * @param deviceSurveyId device survey id
     * @param pageId page id
     * @param devicePageSurveyAnswer device page survey answer
     * @param surveyId survey id (not needed for actual request)
     */
    fun submitSurveyAnswer(
        deviceId: UUID,
        deviceSurveyId: UUID,
        pageId: UUID,
        devicePageSurveyAnswer: DevicePageSurveyAnswer,
        surveyId: UUID
    ) {
        api.submitSurveyAnswer(
            deviceId = deviceId,
            deviceSurveyId = deviceSurveyId,
            pageId = pageId,
            devicePageSurveyAnswer = devicePageSurveyAnswer
        )

        // add it as closable to resource which can delete the answer
        surveyAnswersResource.list(
            surveyId = surveyId,
            pageId = pageId
        ).forEach {
            if (submittedClosableAnswersIDs.contains(it.id)) {
                return@forEach
            }
            surveyAnswersResource.addClosable(it)
            surveyAnswersResource.answerToSurvey[it.id!!] = surveyId
            submittedClosableAnswersIDs.add(it.id)
        }
    }

    /**
     * Checks that device fails to submit survey answer
     *
     * @param deviceId device id
     * @param deviceSurveyId device survey id
     * @param pageId page id
     * @param devicePageSurveyAnswer device page survey answer
     * @param expectedStatusCode expected status code
     */
    fun assertCreateFail(
        deviceId: UUID,
        deviceSurveyId: UUID,
        pageId: UUID,
        devicePageSurveyAnswer: DevicePageSurveyAnswer,
        expectedStatusCode: Int
    ) {
        try {
            api.submitSurveyAnswer(
                deviceId = deviceId,
                deviceSurveyId = deviceSurveyId,
                pageId = pageId,
                devicePageSurveyAnswer = devicePageSurveyAnswer
            )
            fail("Crating answer should have failed")
        } catch (e: ClientException) {
            assertClientExceptionStatus(expectedStatusCode, e)
        }
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