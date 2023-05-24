package fi.metatavu.oss.api.impl.pages

import fi.metatavu.oss.api.impl.AbstractApi
import fi.metatavu.oss.api.impl.UserRole
import fi.metatavu.oss.api.impl.devicesurveys.DeviceSurveyController
import fi.metatavu.oss.api.impl.layouts.LayoutController
import fi.metatavu.oss.api.impl.pages.answers.PageAnswerController
import fi.metatavu.oss.api.impl.surveys.SurveyController
import fi.metatavu.oss.api.model.Page
import fi.metatavu.oss.api.spec.PagesApi
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.*
import javax.annotation.security.RolesAllowed
import javax.enterprise.context.RequestScoped
import javax.inject.Inject
import javax.ws.rs.core.Response

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
    lateinit var layoutController: LayoutController

    @Inject
    lateinit var deviceSurveyController: DeviceSurveyController

    @Inject
    lateinit var pageAnswerController: PageAnswerController

    @Inject
    lateinit var vertx: io.vertx.core.Vertx

    @ReactiveTransactional
    @RolesAllowed(UserRole.MANAGER.name)
    override fun listSurveyPages(surveyId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val survey = surveyController.findSurvey(surveyId) ?: return@async createNotFoundWithMessage(SURVEY, surveyId)
        val (pages, count) = pagesController.listPages(survey)

        createOk(pagesTranslator.translate(pages), count)
    }.asUni()

    @ReactiveTransactional
    @RolesAllowed(UserRole.MANAGER.name)
    override fun createSurveyPage(surveyId: UUID, page: Page): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
            val survey = surveyController.findSurvey(surveyId) ?: return@async createNotFoundWithMessage(SURVEY, surveyId)
            val layout = layoutController.find(page.layoutId) ?: return@async createBadRequest(
                "No layout found!"
            )

            createOk(
                pagesTranslator.translate(
                    pagesController.createPage(
                        survey = survey,
                        page = page,
                        layout = layout,
                        userId = userId
                    )
                )
            )
        }.asUni()

    @ReactiveTransactional
    @RolesAllowed(UserRole.MANAGER.name)
    override fun findSurveyPage(surveyId: UUID, pageId: UUID): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val survey = surveyController.findSurvey(surveyId) ?: return@async createNotFoundWithMessage(SURVEY, surveyId)
            val page = pagesController.findPage(pageId) ?: return@async createNotFoundWithMessage(PAGE, pageId)

            if (page.survey.id != survey.id) return@async createNotFoundWithMessage(PAGE, pageId)

            createOk(pagesTranslator.translate(page))
        }.asUni()

    @ReactiveTransactional
    @RolesAllowed(UserRole.MANAGER.name)
    override fun updateSurveyPage(surveyId: UUID, pageId: UUID, page: Page): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
            val survey = surveyController.findSurvey(surveyId) ?: return@async createNotFoundWithMessage(SURVEY, surveyId)
            val existingPage = pagesController.findPage(pageId) ?: return@async createNotFoundWithMessage(PAGE, pageId)

            if (existingPage.survey.id != survey.id) return@async createNotFoundWithMessage(PAGE, pageId)

            val layout = layoutController.find(page.layoutId) ?: return@async createBadRequest(
                "No layout found!"
            )

            canBeModified(existingPage).let { if (it != null) return@async it }

            val updatedPage = pagesController.updatePage(
                existingPage = existingPage,
                updateData = page,
                layout = layout,
                userId = userId
            )

            createOk(pagesTranslator.translate(updatedPage))
        }.asUni()

    @ReactiveTransactional
    @RolesAllowed(UserRole.MANAGER.name)
    override fun deleteSurveyPage(surveyId: UUID, pageId: UUID): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val survey =
                surveyController.findSurvey(surveyId) ?: return@async createNotFoundWithMessage(SURVEY, surveyId)
            val existingPage = pagesController.findPage(pageId) ?: return@async createNotFoundWithMessage(PAGE, pageId)

            if (existingPage.survey.id != survey.id) return@async createNotFoundWithMessage(PAGE, pageId)
            canBeModified(existingPage).let { if (it != null) return@async it }

            pagesController.deletePage(existingPage)
            createNoContent()
        }.asUni()

    /**
     * Checks if page can be modified. Does not apply for staging environment
     *
     * @param page page
     * @return if is published on any device
     */
    private suspend fun canBeModified(page: PageEntity): Response? {
        if (isStaging || isTest) return null
        val (foundDeviceSurveys) = deviceSurveyController.listDeviceSurveysBySurvey(page.survey.id)
        val hasAnswers = pageAnswerController.list(page)

        if (foundDeviceSurveys.isNotEmpty()) {
            val deviceIds = foundDeviceSurveys.map { it.device.id }.toSet()
            return createBadRequest("Survey is assigned to devices $deviceIds")
        }

        if (hasAnswers.isNotEmpty()) {
            return createBadRequest("There have been answers submitted for this page")
        }

        return null
    }
}