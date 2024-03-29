package fi.metatavu.oss.api.impl.surveys

import fi.metatavu.oss.api.impl.AbstractApi
import fi.metatavu.oss.api.impl.UserRole
import fi.metatavu.oss.api.impl.devicesurveys.DeviceSurveyController
import fi.metatavu.oss.api.model.Survey
import fi.metatavu.oss.api.model.SurveyStatus
import fi.metatavu.oss.api.spec.SurveysApi
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.core.Vertx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import java.util.*
import javax.annotation.security.RolesAllowed
import javax.enterprise.context.RequestScoped
import javax.inject.Inject
import javax.ws.rs.core.Response

@RequestScoped
@Suppress ("unused")
@OptIn(ExperimentalCoroutinesApi::class)
class SurveysApiImpl : SurveysApi, AbstractApi() {

    @Inject
    lateinit var surveyController: SurveyController

    @Inject
    lateinit var surveyTranslator: SurveyTranslator

    @Inject
    lateinit var deviceSurveyController: DeviceSurveyController

    @Inject
    lateinit var vertx: Vertx

    @ReactiveTransactional
    @RolesAllowed(UserRole.MANAGER.name, UserRole.CONSUMER_DISPLAY.name)
    override fun listSurveys(firstResult: Int?, maxResults: Int?, status: SurveyStatus?): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val (start, end) = firstMaxToRange(firstResult, maxResults)
        val (surveys, count) = surveyController.listSurveys(
            rangeStart = start,
            rangeEnd = end,
            status = status
        )
        val surveysTranslated = surveyTranslator.translate(surveys)
        createOk(surveysTranslated, count)
    }.asUni()

    @ReactiveTransactional
    @RolesAllowed(UserRole.MANAGER.name)
    override fun createSurvey(survey: Survey): Uni<Response> {
        return CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)

            if (survey.timeout <= 0) {
                return@async createBadRequest("Negative timeout is not allowed")
            }

            val createdSurvey = surveyController.createSurvey(survey, userId)

            createOk(surveyTranslator.translate(createdSurvey))
        }.asUni()
    }

    @ReactiveTransactional
    @RolesAllowed(UserRole.MANAGER.name, UserRole.CONSUMER_DISPLAY.name)
    override fun findSurvey(surveyId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val foundSurvey =
            surveyController.findSurvey(surveyId) ?: return@async createNotFoundWithMessage(
                target = SURVEY,
                id = surveyId
            )
        createOk(surveyTranslator.translate(foundSurvey))
    }.asUni()

    @ReactiveTransactional
    @RolesAllowed(UserRole.MANAGER.name)
    override fun updateSurvey(surveyId: UUID, survey: Survey): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        val foundSurvey =
            surveyController.findSurvey(surveyId) ?: return@async createNotFoundWithMessage(
                target = SURVEY,
                id = surveyId
            )

        if (survey.timeout <= 0) {
            return@async createBadRequest("Negative timeout is not allowed")
        }

        val updatedSurvey = surveyController.updateSurvey(foundSurvey, survey, userId)

        deviceSurveyController.notifyDevicesOfSurveyUpdate(updatedSurvey.id)

        createOk(surveyTranslator.translate(updatedSurvey))
    }.asUni()

    @ReactiveTransactional
    @RolesAllowed(UserRole.MANAGER.name)
    override fun deleteSurvey(surveyId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val foundSurvey = surveyController.findSurvey(surveyId) ?: return@async createNotFoundWithMessage(
            target = SURVEY,
            id = surveyId
        )

        val (foundDeviceSurveys) = deviceSurveyController.listDeviceSurveys(surveyId = surveyId)

        if (foundDeviceSurveys.isNotEmpty()) {
            val deviceIds = foundDeviceSurveys.map { it.device.id }.toSet()

            return@async createBadRequest("Survey is assigned to devices $deviceIds")
        }

        surveyController.deleteSurvey(foundSurvey)

        createNoContent()
    }.asUni()
}