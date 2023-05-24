package fi.metatavu.oss.api.impl.pages.answers

import fi.metatavu.oss.api.impl.AbstractApi
import fi.metatavu.oss.api.impl.UserRole
import fi.metatavu.oss.api.impl.pages.PageEntity
import fi.metatavu.oss.api.impl.pages.PagesController
import fi.metatavu.oss.api.impl.surveys.SurveyController
import fi.metatavu.oss.api.spec.SurveyAnswersApi
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.core.Vertx
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
class SurveyAnswersApiImpl : SurveyAnswersApi, AbstractApi() {

    @Inject
    lateinit var surveyController: SurveyController

    @Inject
    lateinit var pageAnswerController: PageAnswerController

    @Inject
    lateinit var pagesController: PagesController

    @Inject
    lateinit var pageAnswerTranslator: PageAnswerTranslator

    @Inject
    lateinit var vertx: Vertx

    @RolesAllowed(UserRole.MANAGER.name)
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun listSurveyPageAnswers(
        surveyId: UUID,
        pageId: UUID
    ): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val pageOrError = findSurveyPage(
            pageId = pageId,
            surveyId = surveyId
        )
        pageOrError.second.let { if (it != null) return@async it }
        val page = pageOrError.first ?: return@async createNotFoundWithMessage(
            target = PAGE,
            id = pageId
        )

        val answers = pageAnswerController.list(page = page)
        return@async createOk(answers.map { pageAnswerTranslator.translate(it) }, answers.size.toLong())
    }.asUni()

    @RolesAllowed(UserRole.MANAGER.name)
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun findSurveyPageAnswer(
        surveyId: UUID,
        pageId: UUID,
        answerId: UUID
    ): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val pageOrError = findSurveyPage(
            pageId = pageId,
            surveyId = surveyId
        )
        pageOrError.second.let { if (it != null) return@async it }
        val page = pageOrError.first ?: return@async createNotFoundWithMessage(
            target = PAGE,
            id = pageId
        )

        val answer = pageAnswerController.find(
            page = page,
            answerId = answerId
        ) ?: return@async createNotFoundWithMessage(
            target = ANSWER,
            id = answerId
        )

        return@async createOk(pageAnswerTranslator.translate(answer))
    }.asUni()

    @RolesAllowed(UserRole.MANAGER.name)
    @ReactiveTransactional
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun deleteSurveyPageAnswer(
        surveyId: UUID,
        pageId: UUID,
        answerId: UUID
    ): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        if (!isTest) return@async createForbidden("Allowed only for testing purposes")
        val pageOrError = findSurveyPage(
            pageId = pageId,
            surveyId = surveyId
        )
        pageOrError.second.let { if (it != null) return@async it }
        val page = pageOrError.first ?: return@async createNotFoundWithMessage(
            target = PAGE,
            id = pageId
        )

        val answer = pageAnswerController.find(
            page = page,
            answerId = answerId
        ) ?: return@async createNotFoundWithMessage(
            target = ANSWER,
            id = answerId
        )

        pageAnswerController.delete(answer)
        return@async createAccepted(null)
    }.asUni()


    /**
     * Finds survey page, checking that all entities are present
     *
     * @param pageId page id
     * @param surveyId survey id
     * @return pair of page and error response if any
     */
    private suspend fun findSurveyPage(pageId: UUID, surveyId: UUID): Pair<PageEntity?, Response?> {
        val survey = surveyController.findSurvey(surveyId) ?: return null to createNotFoundWithMessage(
            target = SURVEY,
            id = surveyId
        )

        val page = pagesController.findPage(pageId) ?: return null to createNotFoundWithMessage(
            target = PAGE,
            id = pageId
        )

        if (page.survey.id != survey.id) {
            return null to createNotFoundWithMessage(
                target = PAGE,
                id = pageId
            )
        }

        return page to null
    }
}