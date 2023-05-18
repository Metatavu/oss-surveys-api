package fi.metatavu.oss.api.impl.pages

import fi.metatavu.oss.api.impl.layouts.LayoutEntity
import fi.metatavu.oss.api.impl.pages.questions.PageQuestionController
import fi.metatavu.oss.api.impl.surveys.SurveyEntity
import fi.metatavu.oss.api.model.Page
import io.smallrye.mutiny.coroutines.awaitSuspending
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import java.util.*

/**
 * Controller for survey Pages
 */
@ApplicationScoped
class PagesController {

    @Inject
    lateinit var pageRepository: PageRepository

    @Inject
    lateinit var pagePropertyRepository: PagePropertyRepository

    @Inject
    lateinit var pageQuestionController: PageQuestionController

    /**
     * Creates a page for the survey and fills in its properties
     *
     * @param page page data
     * @param survey survey
     * @param userId creator id
     * @return created page
     */
    suspend fun createPage(page: Page, survey: SurveyEntity, layout: LayoutEntity, userId: UUID): PageEntity {
        val createdPage = pageRepository.create(
            id = UUID.randomUUID(),
            title = page.title,
            survey = survey,
            layout = layout,
            orderNumber = page.orderNumber,
            nextButtonVisible = page.nextButtonVisible,
            userId = userId
        )

        page.properties?.forEach {
            pagePropertyRepository.create(
                id = UUID.randomUUID(),
                key = it.key,
                value = it.value,
                page = createdPage
            )
        }

        if (page.question != null) {
            pageQuestionController.create(
                pageQuestion = page.question,
                page = createdPage
            )
        }

        return createdPage
    }

    /**
     * Lists pages by survey
     *
     * @param survey survey
     * @return list of pages and count
     */
    suspend fun listPages(survey: SurveyEntity): Pair<List<PageEntity>, Long> {
        return pageRepository.listBySurvey(survey)
    }

    /**
     * Lists pages by layouts
     *
     * @param layout layout
     * @return list of pages and count
     */
    suspend fun listPages(layout: LayoutEntity): Pair<List<PageEntity>, Long> {
        return pageRepository.listByLayout(layout)
    }

    /**
     * Finds a page by id
     *
     * @param pageId page id
     * @return found page or null if not found
     */
    suspend fun findPage(pageId: UUID): PageEntity? {
        return pageRepository.findById(pageId).awaitSuspending()
    }

    /**
     * Updates a page and reassigns the properties
     *
     * @param existingPage existing page
     * @param updateData update data
     * @param layout new layout
     * @param userId modifier id
     * @return updated page
     */
    suspend fun updatePage(existingPage: PageEntity, updateData: Page, layout: LayoutEntity, userId: UUID): PageEntity {
        existingPage.title = updateData.title
        existingPage.orderNumber = updateData.orderNumber
        existingPage.layout = layout
        existingPage.nextButtonVisible = updateData.nextButtonVisible
        existingPage.lastModifierId = userId

        pagePropertyRepository.listByPage(existingPage).forEach { pagePropertyRepository.deleteSuspending(it) }
        updateData.properties?.forEach {
            pagePropertyRepository.create(
                id = UUID.randomUUID(),
                key = it.key,
                value = it.value,
                page = existingPage
            )
        }

        pageQuestionController.update(
            questionToUpdate = pageQuestionController.find(existingPage),
            newQuestion = updateData.question,
            page = existingPage
        )
        return pageRepository.persistSuspending(existingPage)
    }

    /**
     * Deletes page and its properties
     *
     * @param page page to delete
     */
    suspend fun deletePage(page: PageEntity) {
        pageQuestionController.find(page).let { if (it != null) pageQuestionController.delete(it) }
        pagePropertyRepository.listByPage(page).forEach { pagePropertyRepository.deleteSuspending(it) }
        pageRepository.deleteSuspending(page)
    }

}
