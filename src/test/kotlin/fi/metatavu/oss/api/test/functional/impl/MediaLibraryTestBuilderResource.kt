package fi.metatavu.oss.api.test.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.oss.api.test.functional.TestBuilder
import fi.metatavu.oss.api.test.functional.settings.ApiTestSettings
import fi.metatavu.oss.test.client.apis.MediaLibraryApi
import fi.metatavu.oss.test.client.infrastructure.ApiClient
import fi.metatavu.oss.test.client.models.MediaFile
import fi.metatavu.oss.test.client.models.Page

/**
 * Test builder resource for MediaLibrary API
 */
class MediaLibraryTestBuilderResource(
    testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
) : ApiTestBuilderResource<Page, ApiClient>(testBuilder, apiClient) {

    override fun clean(t: Page?) {
        // resource does not create entities -> nothing to clean
    }

    override fun getApi(): MediaLibraryApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return MediaLibraryApi(ApiTestSettings.apiBasePath)
    }

    fun list(path: String): Array<MediaFile> {
        return api.listMediaFiles(path)
    }
}