package fi.metatavu.oss.api.impl.pages

import fi.metatavu.oss.api.impl.AbstractApi
import fi.metatavu.oss.api.impl.UserRole
import fi.metatavu.oss.api.impl.surveys.SurveyController
import fi.metatavu.oss.api.model.Page
import fi.metatavu.oss.api.spec.PagesApi
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.kotlin.coroutines.dispatcher
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import java.util.*

@RequestScoped
@Suppress("unused")
@OptIn(ExperimentalCoroutinesApi::class)
class PagesApiImpl : PagesApi, AbstractApi() {

    @Inject
    lateinit var pagesController: PagesController

    @Inject
    lateinit var surveyController: SurveyController

    @Inject
    lateinit var pagesTranslator: PagesTranslator

    @Inject
    lateinit var vertx: io.vertx.core.Vertx

    @WithTransaction
    @RolesAllowed(UserRole.MANAGER.name, UserRole.CONSUMER_DISPLAY.name)
    override fun listSurveyPages(surveyId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val survey = surveyController.findSurvey(surveyId) ?: return@async createNotFoundWithMessage(SURVEY, surveyId)
        val (pages, count) = pagesController.listPages(survey)

        createOk(pagesTranslator.translate(pages), count.toLong())
    }.asUni()

    @WithTransaction
    @RolesAllowed(UserRole.MANAGER.name)
    override fun createSurveyPage(surveyId: UUID, page: Page): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
            val survey = surveyController.findSurvey(surveyId) ?: return@async createNotFoundWithMessage(SURVEY, surveyId)

            createOk(
                pagesTranslator.translate(
                    pagesController.createPage(
                        survey = survey,
                        page = page,
                        userId = userId
                    )
                )
            )
        }.asUni()

    @WithTransaction
    @RolesAllowed(UserRole.MANAGER.name, UserRole.CONSUMER_DISPLAY.name)
    override fun findSurveyPage(surveyId: UUID, pageId: UUID): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val survey = surveyController.findSurvey(surveyId) ?: return@async createNotFoundWithMessage(SURVEY, surveyId)
            val page = pagesController.findPage(pageId) ?: return@async createNotFoundWithMessage(PAGE, pageId)

            if (page.survey != survey) return@async createNotFoundWithMessage(PAGE, pageId)

            createOk(pagesTranslator.translate(page))
        }.asUni()

    @WithTransaction
    @RolesAllowed(UserRole.MANAGER.name)
    override fun updateSurveyPage(surveyId: UUID, pageId: UUID, page: Page): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
            val survey = surveyController.findSurvey(surveyId) ?: return@async createNotFoundWithMessage(SURVEY, surveyId)
            val existingPage = pagesController.findPage(pageId) ?: return@async createNotFoundWithMessage(PAGE, pageId)

            if (existingPage.survey != survey) return@async createNotFoundWithMessage(PAGE, pageId)

            val updatedPage = pagesController.updatePage(
                existingPage = existingPage,
                updateData = page,
                userId = userId
            )

            createOk(pagesTranslator.translate(updatedPage))
        }.asUni()

    @WithTransaction
    @RolesAllowed(UserRole.MANAGER.name)
    override fun deleteSurveyPage(surveyId: UUID, pageId: UUID): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val survey =
                surveyController.findSurvey(surveyId) ?: return@async createNotFoundWithMessage(SURVEY, surveyId)
            val existingPage = pagesController.findPage(pageId) ?: return@async createNotFoundWithMessage(PAGE, pageId)

            if (existingPage.survey != survey) return@async createNotFoundWithMessage(PAGE, pageId)

            pagesController.deletePage(existingPage)
            createNoContent()
        }.asUni()
}