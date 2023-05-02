package fi.metatavu.oss.api.impl.surveys

import fi.metatavu.oss.api.impl.AbstractApi
import fi.metatavu.oss.api.impl.UserRole
import fi.metatavu.oss.api.model.Survey
import fi.metatavu.oss.api.spec.SurveysApi
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.core.Vertx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import java.util.*
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response

@RequestScoped
@Suppress ("unused")
@OptIn(ExperimentalCoroutinesApi::class)
class SurveysApiImpl : SurveysApi, AbstractApi() {

    @Inject
    lateinit var surveyController: SurveyController

    @Inject
    lateinit var surveyTranslator: SurveyTranslator

    @Inject
    lateinit var vertx: Vertx

    @WithTransaction
    @RolesAllowed(UserRole.MANAGER.name, UserRole.CONSUMER_DISPLAY.name)
    override fun listSurveys(firstResult: Int?, maxResults: Int?): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val (surveys, count) = surveyController.listSurveys(firstResult, maxResults)
        val surveysTranslated = surveyTranslator.translate(surveys)
        createOk(surveysTranslated, count)
    }.asUni()

    @WithTransaction
    @RolesAllowed(UserRole.MANAGER.name)
    override fun createSurvey(survey: Survey): Uni<Response> {
        return CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
            val createdSurvey = surveyController.createSurvey(survey, userId)

            createOk(surveyTranslator.translate(createdSurvey))
        }.asUni()
    }

    @WithTransaction
    @RolesAllowed(UserRole.MANAGER.name, UserRole.CONSUMER_DISPLAY.name)
    override fun findSurvey(surveyId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val foundSurvey =
            surveyController.findSurvey(surveyId) ?: return@async createNotFoundWithMessage(
                target = SURVEY,
                id = surveyId
            )
        createOk(surveyTranslator.translate(foundSurvey))
    }.asUni()

    @WithTransaction
    @RolesAllowed(UserRole.MANAGER.name)
    override fun updateSurvey(surveyId: UUID, survey: Survey): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        val foundSurvey =
            surveyController.findSurvey(surveyId) ?: return@async createNotFoundWithMessage(
                target = SURVEY,
                id = surveyId
            )

        val updatedSurvey = surveyController.updateSurvey(foundSurvey, survey, userId)

        createOk(surveyTranslator.translate(updatedSurvey))
    }.asUni()

    @WithTransaction
    @RolesAllowed(UserRole.MANAGER.name)
    override fun deleteSurvey(surveyId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val foundSurvey = surveyController.findSurvey(surveyId) ?: return@async createNotFoundWithMessage(
            target = SURVEY,
            id = surveyId
        )

        surveyController.deleteSurvey(foundSurvey)

        createNoContent()
    }.asUni()
}