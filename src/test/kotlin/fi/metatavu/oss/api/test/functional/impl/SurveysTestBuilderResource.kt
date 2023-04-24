package fi.metatavu.oss.api.test.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.oss.api.test.functional.TestBuilder
import fi.metatavu.oss.api.test.functional.settings.ApiTestSettings
import fi.metatavu.oss.test.client.apis.SurveysApi
import fi.metatavu.oss.test.client.infrastructure.ApiClient
import fi.metatavu.oss.test.client.infrastructure.ClientException
import fi.metatavu.oss.test.client.models.Survey
import fi.metatavu.oss.test.client.models.SurveyStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import java.util.*

/**
 * Test resources for surveys
 */
class SurveysTestBuilderResource(
    testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
) : ApiTestBuilderResource<Survey, ApiClient>(testBuilder, apiClient) {
    override fun clean(t: Survey?) {
        api.deleteSurvey(t!!.id!!)
    }

    override fun getApi(): SurveysApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return SurveysApi(ApiTestSettings.apiBasePath)
    }

    fun create(survey: Survey): Survey {
        val created = api.createSurvey(survey)
        return addClosable(created)
    }

    fun createDefault(): Survey {
        val created = api.createSurvey(Survey(title = "default survey"))
        return addClosable(created)
    }

    fun list(
        firstResult: Int?,
        maxResults: Int?,
        status: SurveyStatus?
    ): Array<Survey> {
        return api.listSurveys(
            firstResult = firstResult,
            maxResults = maxResults,
            status = status
        )
    }

    fun find(surveyId: UUID): Survey {
        return api.findSurvey(surveyId)
    }

    fun update(surveyId: UUID, newSurvey: Survey): Survey {
        return api.updateSurvey(surveyId, newSurvey)
    }

    fun delete(surveyId: UUID) {
        api.deleteSurvey(surveyId)
        removeCloseable { closable ->
            if (closable !is Survey) {
                return@removeCloseable false
            }

            closable.id!! == surveyId
        }
    }

    fun assertListFail(expectedStatus: Int) {
        try {
            api.listSurveys(firstResult = null, maxResults = null)
            fail("List should have failed")
        } catch (e: ClientException) {
            assertEquals(expectedStatus, e.statusCode)
        }
    }

    fun assertFindFail(expectedStatus: Int, surveyId: UUID) {
        try {
            api.findSurvey(surveyId)
            fail("Find should have failed")
        } catch (e: ClientException) {
            assertEquals(expectedStatus, e.statusCode)
        }
    }

    fun assertCreateFail(expectedStatus: Int, survey: Survey) {
        try {
            api.createSurvey(survey)
            fail("Create should have failed")
        } catch (e: ClientException) {
            assertEquals(expectedStatus, e.statusCode)
        }
    }

    fun assertUpdateFail(expectedStatus: Int, surveyId: UUID, newSurvey: Survey) {
        try {
            api.updateSurvey(surveyId, newSurvey)
            fail("Update should have failed")
        } catch (e: ClientException) {
            assertEquals(expectedStatus, e.statusCode)
        }
    }

    fun assertDeleteFail(expectedStatus: Int, surveyId: UUID) {
        try {
            api.deleteSurvey(surveyId)
            fail("Delete should have failed")
        } catch (e: ClientException) {
            assertEquals(expectedStatus, e.statusCode)
        }
    }
}