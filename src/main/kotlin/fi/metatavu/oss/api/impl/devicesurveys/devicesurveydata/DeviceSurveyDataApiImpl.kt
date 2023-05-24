package fi.metatavu.oss.api.impl.devicesurveys.devicesurveydata

import fi.metatavu.oss.api.impl.AbstractApi
import fi.metatavu.oss.api.impl.devices.DeviceController
import fi.metatavu.oss.api.impl.devicesurveys.DeviceSurveyController
import fi.metatavu.oss.api.impl.pages.PagesController
import fi.metatavu.oss.api.impl.pages.answers.PageAnswerController
import fi.metatavu.oss.api.impl.pages.questions.PageQuestionController
import fi.metatavu.oss.api.model.DevicePageSurveyAnswer
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import java.util.*
import javax.enterprise.context.RequestScoped
import javax.inject.Inject
import javax.ws.rs.core.Response

/**
 * API implementation for Device survey data API (accessed by devices to get the survey display data)
 */
@RequestScoped
@Suppress("unused")
@OptIn(ExperimentalCoroutinesApi::class)
class DeviceSurveyDataApiImpl: fi.metatavu.oss.api.spec.DeviceDataApi, AbstractApi() {

    @Inject
    lateinit var deviceSurveyController: DeviceSurveyController

    @Inject
    lateinit var deviceSurveyDataTranslator: DeviceSurveyDataTranslator

    @Inject
    lateinit var deviceController: DeviceController

    @Inject
    lateinit var pageAnswerController: PageAnswerController

    @Inject
    lateinit var pageQuestionController: PageQuestionController

    @Inject
    lateinit var pagesController: PagesController

    @Inject
    lateinit var vertx: Vertx

    override fun findDeviceDataSurvey(deviceId: UUID, deviceSurveyId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        if (!isAuthorizedDevice(deviceId)) return@async createUnauthorized(UNAUTHORIZED)
        val device = deviceController.findDevice(deviceId) ?: return@async createNotFoundWithMessage(
            target = DEVICE,
            id = deviceId
        )

        val deviceSurvey = deviceSurveyController.findDeviceSurvey(deviceSurveyId) ?: return@async createNotFoundWithMessage(
            target = DEVICE_SURVEY,
            id = deviceSurveyId
        )

        if (deviceSurvey.device != device) {
            return@async createBadRequest("Device in path and device survey object does not match")
        }

        return@async createOk(deviceSurveyDataTranslator.translate(deviceSurvey))
    }.asUni()

    override fun listDeviceDataSurveys(deviceId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        if (!isAuthorizedDevice(deviceId)) return@async createUnauthorized(UNAUTHORIZED)
        val device = deviceController.findDevice(deviceId) ?: return@async createNotFoundWithMessage(
            target = DEVICE,
            id = deviceId
        )

        val ( deviceSurveys, count ) = deviceSurveyController.listDeviceSurveysByDevice(device.id)
        return@async createOk(deviceSurveys.map { deviceSurveyDataTranslator.translate(it) }, count)
    }.asUni()

    @ReactiveTransactional
    override fun submitSurveyAnswer(deviceId: UUID, deviceSurveyId: UUID, pageId: UUID, devicePageSurveyAnswer: DevicePageSurveyAnswer): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        if (!isAuthorizedDevice(deviceId)) return@async createUnauthorized(UNAUTHORIZED)
        if (devicePageSurveyAnswer.answer.isNullOrEmpty()) {
            return@async createBadRequest("Answer is required")
        }

        val device = deviceController.findDevice(deviceId) ?: return@async createNotFoundWithMessage(
            target = DEVICE,
            id = deviceId
        )

        val deviceSurvey = deviceSurveyController.findDeviceSurvey(deviceSurveyId)
            ?: return@async createNotFoundWithMessage(
                target = DEVICE_SURVEY,
                id = deviceSurveyId
            )

        val page = pagesController.findPage(pageId) ?: return@async createNotFoundWithMessage(
            target = PAGE,
            id = pageId
        )

        if (page.survey != deviceSurvey.survey) {
            return@async createNotFoundWithMessage(
                target = PAGE,
                id = pageId)
        }

        val question = pageQuestionController.find(page) ?: return@async createNotFound(
            "No question found for page $pageId"
        )


        if (deviceSurvey.device != device) {
            return@async createBadRequest("Device in path and device survey object does not match")
        }

        try {
            pageAnswerController.create(
                deviceSurvey = deviceSurvey,
                page = page,
                pageQuestion = question,
                answer = devicePageSurveyAnswer
            )
        } catch (e: Exception) {
            return@async createBadRequest("Invalid answer")
        }

        return@async createAccepted(null)
    }.asUni()

}