package fi.metatavu.oss.api.impl.pages

import fi.metatavu.oss.api.impl.surveys.SurveyEntity
import fi.metatavu.oss.api.model.Page
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
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

    /**
     * Creates a page for the survey and fills in its properties
     *
     * @param page page data
     * @param survey survey
     * @param userId creator id
     * @return created page
     */
    suspend fun createPage(page: Page, survey: SurveyEntity, userId: UUID): PageEntity {
        val createdPage = pageRepository.create(
            id = UUID.randomUUID(),
            title = page.title,
            html = page.html,
            survey = survey,
            userId = userId
        )

        page.properties?.forEach {
            pagePropertyRepository.create(
                id = UUID.randomUUID(),
                key = it.key,
                value = it.value,
                type = it.type,
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
    suspend fun listPages(survey: SurveyEntity): Pair<List<PageEntity>, Int> {
        return pageRepository.listBySurvey(survey)
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
     * @param userId modifier id
     * @return updated page
     */
    suspend fun updatePage(existingPage: PageEntity, updateData: Page, userId: UUID): PageEntity {
        existingPage.html = updateData.html
        existingPage.title = updateData.title
        existingPage.lastModifierId = userId

        pagePropertyRepository.listByPage(existingPage).forEach { pagePropertyRepository.deleteSuspending(it) }
        updateData.properties?.forEach {
            pagePropertyRepository.create(
                id = UUID.randomUUID(),
                key = it.key,
                value = it.value,
                type = it.type,
                page = existingPage
            )
        }

        return pageRepository.persistSuspending(existingPage)
    }

    /**
     * Deletes page and its properties
     *
     * @param page page to delete
     */
    suspend fun deletePage(page: PageEntity) {
        pagePropertyRepository.listByPage(page).forEach { pagePropertyRepository.deleteSuspending(it) }
        pageRepository.deleteSuspending(page)
    }

}
