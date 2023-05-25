package fi.metatavu.oss.api.test.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.oss.api.test.functional.TestBuilder
import fi.metatavu.oss.api.test.functional.settings.ApiTestSettings
import fi.metatavu.oss.test.client.apis.SurveyAnswersApi
import fi.metatavu.oss.test.client.infrastructure.ApiClient
import fi.metatavu.oss.test.client.models.DevicePageSurveyAnswer
import java.util.*

/**
 * Test resource for survey answers api
 */
class SurveyAnswersTestBuilderResource(
    testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
) : ApiTestBuilderResource<DevicePageSurveyAnswer, ApiClient>(testBuilder, apiClient) {

    var answerToSurvey = mutableMapOf<UUID, UUID>()
    override fun clean(t: DevicePageSurveyAnswer?) {
        api.deleteSurveyPageAnswer(
            surveyId = answerToSurvey[t!!.id]!!,
            pageId = t.pageId!!,
            answerId = t.id!!
        )
    }

    override fun getApi(): SurveyAnswersApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return SurveyAnswersApi(ApiTestSettings.apiBasePath)
    }

    /**
     * Deletes answer. Endpoint only for testing
     *
     * @param surveyId survey id
     * @param pageId page id
     * @param answerId answer id
     */
    fun delete(
        surveyId: UUID,
        pageId: UUID,
        answerId: UUID
    ) {
        api.deleteSurveyPageAnswer(
            surveyId = surveyId,
            pageId = pageId,
            answerId = answerId
        )
    }

    /**
     * Lists answers submited for the page
     *
     * @param surveyId survey id
     * @param pageId page id
     * @return list of answers
     */
    fun list(
        surveyId: UUID,
        pageId: UUID
    ): Array<DevicePageSurveyAnswer> {
        return api.listSurveyPageAnswers(
            surveyId = surveyId,
            pageId = pageId
        )
    }

    /**
     * Finds an answer
     *
     * @param surveyId survey id
     * @param pageId page id
     * @param answerId answer id
     * @return found answer
     */
    fun find(
        surveyId: UUID,
        pageId: UUID,
        answerId: UUID
    ): DevicePageSurveyAnswer {
        return api.findSurveyPageAnswer(
            surveyId = surveyId,
            pageId = pageId,
            answerId = answerId
        )
    }

}