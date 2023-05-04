package fi.metatavu.oss.api.test.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.oss.api.test.functional.TestBuilder
import fi.metatavu.oss.api.test.functional.settings.ApiTestSettings
import fi.metatavu.oss.test.client.apis.LayoutsApi
import fi.metatavu.oss.test.client.infrastructure.ApiClient
import fi.metatavu.oss.test.client.models.Layout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import java.util.*

/**
 * Test builds resource for Pages API
 */
class LayoutTestBuilderResource(
    testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
) : ApiTestBuilderResource<Layout, ApiClient>(testBuilder, apiClient) {


    override fun clean(t: Layout?) {
        api.deleteLayout(t?.id!!)
    }

    override fun getApi(): LayoutsApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return LayoutsApi(ApiTestSettings.apiBasePath)
    }

    fun create(layout: Layout): Layout {
        val created = api.createLayout(layout)
        return addClosable(created)
    }

    fun createDefault(): Layout {
        val created = api.createLayout(
            Layout(
                thumbnail = "https://example.com/thumbnail.png",
                name = "name",
                html = "<html></html>"
            )
        )
        return addClosable(created)
    }

    fun list(
        firstResult: Int?,
        maxResults: Int?
    ): Array<Layout> {
        return api.listLayouts(
            firstResult, maxResults
        )
    }

    fun find(layoutId: UUID): Layout {
        return api.findLayout(layoutId)
    }

    fun update(layoutId: UUID, layout: Layout): Layout {
        val updated = api.updateLayout(layoutId, layout)
        return updated
    }

    fun delete(layoutId: UUID) {
        api.deleteLayout(layoutId)
        removeCloseable { closable: Any ->
            if (closable !is Layout) {
                return@removeCloseable false
            }

            val Layout = closable as Layout
            Layout.id == layoutId
        }
    }

    fun assertCreateFail(layout: Layout, expectedStatus: Int) {
        try {
            api.createLayout(layout)
            fail("Expected create to fail")
        } catch (e: fi.metatavu.oss.test.client.infrastructure.ClientException) {
            assertEquals(expectedStatus, e.statusCode)
        }
    }

    fun assertFindFail(layoutId: UUID, expectedStatus: Int) {
        try {
            api.findLayout(layoutId)
            fail("Expected find to fail")
        } catch (e: fi.metatavu.oss.test.client.infrastructure.ClientException) {
            assertEquals(expectedStatus, e.statusCode)
        }
    }

    fun assertUpdateFail(layoutId: UUID, layout: Layout, expectedStatus: Int) {
        try {
            api.updateLayout(layoutId, layout)
            fail("Expected update to fail")
        } catch (e: fi.metatavu.oss.test.client.infrastructure.ClientException) {
            assertEquals(expectedStatus, e.statusCode)
        }
    }

    fun assertDeleteFail(layoutId: UUID, expectedStatus: Int) {
        try {
            api.deleteLayout(layoutId)
            fail("Expected delete to fail")
        } catch (e: fi.metatavu.oss.test.client.infrastructure.ClientException) {
            assertEquals(expectedStatus, e.statusCode)
        }
    }

    fun assertListFail(expectedStatus: Int) {
        try {
            api.listLayouts(firstResult = null, maxResults = null)
            fail("Expected list to fail")
        } catch (e: fi.metatavu.oss.test.client.infrastructure.ClientException) {
            assertEquals(expectedStatus, e.statusCode)
        }
    }
}