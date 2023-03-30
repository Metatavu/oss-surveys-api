package fi.metatavu.oss.api.impl.surveys

import fi.metatavu.oss.api.impl.AbstractApi
import fi.metatavu.oss.api.impl.UserRole
import fi.metatavu.oss.api.model.Survey
import fi.metatavu.oss.api.spec.SurveysApi
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.util.*
import javax.annotation.security.RolesAllowed
import javax.enterprise.context.RequestScoped
import javax.inject.Inject
import javax.ws.rs.core.Response

@RequestScoped
class SurveysApiImpl : SurveysApi, AbstractApi() {

    @Inject
    lateinit var surveyController: SurveyController

    @Inject
    lateinit var surveyTranslator: SurveyTranslator

    @RolesAllowed(UserRole.MANAGER.name, UserRole.CONSUMER_DISPLAY.name)
    override suspend fun listSurveys(firstResult: Int?, maxResults: Int?): Response {
        val (surveys, count) = surveyController.listSurveys(firstResult, maxResults)
        val surveysTranslated = surveyTranslator.translate(surveys.awaitSuspending())
        val counted = count.awaitSuspending()
        return createOk(surveysTranslated, counted)
    }

    @RolesAllowed(UserRole.MANAGER.name)
    override suspend fun createSurvey(survey: Survey): Response {
        val userId = loggedUserId ?: return createUnauthorized(UNAUTHORIZED)
        val createdSurvey = surveyController.createSurvey(survey, userId)
        return createOk(surveyTranslator.translate(createdSurvey.awaitSuspending()))
    }

    @RolesAllowed(UserRole.MANAGER.name, UserRole.CONSUMER_DISPLAY.name)
    override suspend fun findSurvey(surveyId: UUID): Response {
        val foundSurvey = surveyController.findSurvey(surveyId).awaitSuspending() ?: return createNotFoundWithMessage(
            target = SURVEY,
            id = surveyId
        )
        return createOk(surveyTranslator.translate(foundSurvey))
    }

    @RolesAllowed(UserRole.MANAGER.name)
    override suspend fun updateSurvey(surveyId: UUID, survey: Survey): Response {
        val userId = loggedUserId ?: return createUnauthorized(UNAUTHORIZED)
        val foundSurvey = surveyController.findSurvey(surveyId).awaitSuspending() ?: return createNotFoundWithMessage(
            target = SURVEY,
            id = surveyId
        )

        val updatedSurvey = surveyController.updateSurvey(foundSurvey, survey, userId)
        return createOk(surveyTranslator.translate(updatedSurvey.awaitSuspending()))
    }

    @RolesAllowed(UserRole.MANAGER.name)
    override suspend fun deleteSurvey(surveyId: UUID): Response {
        val foundSurvey = surveyController.findSurvey(surveyId).awaitSuspending() ?: return createNotFoundWithMessage(
            target = SURVEY,
            id = surveyId
        )
        surveyController.deleteSurvey(foundSurvey).awaitSuspending()
        return createNoContent()
    }
}