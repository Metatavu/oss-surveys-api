package fi.metatavu.oss.api.test.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.oss.api.test.functional.TestBuilder
import fi.metatavu.oss.api.test.functional.settings.ApiTestSettings
import fi.metatavu.oss.test.client.apis.PagesApi
import fi.metatavu.oss.test.client.infrastructure.ApiClient
import fi.metatavu.oss.test.client.models.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import java.util.*

/**
 * Test builds resource for Pages API
 */
class PagesTestBuilderResource(
    testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
) : ApiTestBuilderResource<Page, ApiClient>(testBuilder, apiClient) {

    private val pageSurveyRelation = mutableMapOf<UUID, UUID>()
    override fun clean(t: Page?) {
        api.deleteSurveyPage(
            pageId = t!!.id!!,
            surveyId = pageSurveyRelation[t.id]!!
        )
    }

    override fun getApi(): PagesApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return PagesApi(ApiTestSettings.apiBasePath)
    }

    fun create(surveyId: UUID, page: Page): Page {
        val created = api.createSurveyPage(surveyId, page)
        pageSurveyRelation[created.id!!] = surveyId
        return addClosable(created)
    }

    fun createDefault(surveyId: UUID, layoutId: UUID, orderNumber: Int = 1): Page {
        val created = api.createSurveyPage(
            surveyId, Page(
                title = "default page",
                layoutId = layoutId,
                orderNumber = orderNumber,
                nextButtonVisible = true
            )
        )
        pageSurveyRelation[created.id!!] = surveyId
        return addClosable(created)
    }

    fun list(
        surveyId: UUID
    ): Array<Page> {
        return api.listSurveyPages(
            surveyId = surveyId
        )
    }

    fun find(surveyId: UUID, pageId: UUID): Page {
        return api.findSurveyPage(surveyId, pageId)
    }

    fun update(surveyId: UUID, pageId: UUID, page: Page): Page {
        return api.updateSurveyPage(surveyId, pageId, page)
    }

    fun delete(surveyId: UUID, pageId: UUID) {
        api.deleteSurveyPage(surveyId, pageId)
        removeCloseable { closable: Any ->
            if (closable !is Page) {
                return@removeCloseable false
            }

            closable.id == pageId
        }
    }

    fun assertCreateFail(surveyId: UUID, layoutId: UUID, expectedStatus: Int) {
        try {
            createDefault(surveyId  = surveyId, layoutId = layoutId)
            fail("Expected create to fail")
        } catch (e: fi.metatavu.oss.test.client.infrastructure.ClientException) {
            assertEquals(expectedStatus, e.statusCode)
        }
    }

    fun assertListFail(surveyId: UUID, expectedStatus: Int) {
        try {
            api.listSurveyPages(surveyId)
            fail("Expected list to fail")
        } catch (e: fi.metatavu.oss.test.client.infrastructure.ClientException) {
            assertEquals(expectedStatus, e.statusCode)
        }
    }

    fun assertFindFail(surveyId: UUID, pageId: UUID, expectedStatus: Int) {
        try {
            api.findSurveyPage(surveyId, pageId)
            fail("Expected find to fail")
        } catch (e: fi.metatavu.oss.test.client.infrastructure.ClientException) {
            assertEquals(expectedStatus, e.statusCode)
        }
    }

    fun assertUpdateFail(surveyId: UUID, pageId: UUID, layoutId: UUID, orderNumber: Int = 1, expectedStatus: Int) {
        try {
            api.updateSurveyPage(
                surveyId, pageId, Page(
                    title = "default page",
                    orderNumber = orderNumber,
                    layoutId = layoutId,
                    nextButtonVisible = false
                )
            )
            fail("Expected update to fail")
        } catch (e: fi.metatavu.oss.test.client.infrastructure.ClientException) {
            assertEquals(expectedStatus, e.statusCode)
        }
    }

    fun assertDeleteFail(surveyId: UUID, pageId: UUID, expectedStatus: Int) {
        try {
            api.deleteSurveyPage(surveyId, pageId)
            fail("Expected delete to fail")
        } catch (e: fi.metatavu.oss.test.client.infrastructure.ClientException) {
            assertEquals(expectedStatus, e.statusCode)
        }
    }
}