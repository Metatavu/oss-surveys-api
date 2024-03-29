package fi.metatavu.oss.api.test.functional.auth

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenTestBuilderAuthentication
import fi.metatavu.oss.api.test.functional.impl.*
import fi.metatavu.oss.api.test.functional.settings.ApiTestSettings
import fi.metatavu.oss.test.client.infrastructure.ApiClient


/**
 * Test builder authentication
 *
 * @author Jari Nykänen
 * @author Antti Leppä
 *
 * @param testBuilder test builder instance
 * @param accessTokenProvider access token provider
 */
class TestBuilderAuthentication(
    private val testBuilder: fi.metatavu.oss.api.test.functional.TestBuilder,
    accessTokenProvider: AccessTokenProvider
) : AccessTokenTestBuilderAuthentication<ApiClient>(testBuilder, accessTokenProvider) {

    private var accessTokenProvider: AccessTokenProvider? = accessTokenProvider

    val surveys = SurveysTestBuilderResource(testBuilder, this.accessTokenProvider, createClient())
    val devices = DevicesTestBuilderResource(testBuilder, this.accessTokenProvider, createClient())
    val deviceRequests = DeviceRequestsTestBuilderResource(testBuilder, this.accessTokenProvider, createClient())
    val pages = PagesTestBuilderResource(testBuilder, this.accessTokenProvider, createClient())
    val deviceSurveys = DeviceSurveysTestBuilderResource(testBuilder, this.accessTokenProvider, createClient())
    val layouts = LayoutTestBuilderResource(testBuilder, this.accessTokenProvider, createClient())
    val surveyAnswers = SurveyAnswersTestBuilderResource(testBuilder, this.accessTokenProvider, createClient())
    val deviceData = DeviceSurveyDatasTestBuilderResource(testBuilder, this.accessTokenProvider, createClient(), surveyAnswers)
    val mediaLibrary = MediaLibraryTestBuilderResource(testBuilder, this.accessTokenProvider, createClient())

    override fun createClient(authProvider: AccessTokenProvider): ApiClient {
        val result = ApiClient(ApiTestSettings.apiBasePath)
        ApiClient.accessToken = authProvider.accessToken
        return result
    }

}