package fi.metatavu.oss.api.impl.pages

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import fi.metatavu.oss.api.impl.layouts.LayoutEntity
import fi.metatavu.oss.api.impl.surveys.SurveyEntity
import io.quarkus.panache.common.Sort
import java.util.*
import javax.enterprise.context.ApplicationScoped

/**
 * A repository class for pages
 */
@ApplicationScoped
class PageRepository: AbstractRepository<PageEntity, UUID>() {

    /**
     * Lists pages by survey
     *
     * @param survey survey
     * @return list of pages and count
     */
    suspend fun listBySurvey(survey: SurveyEntity): Pair<List<PageEntity>, Long> {
        return applyPagingToQuery(
            query = find("survey = ?1", Sort.ascending("orderNumber"), survey),
            page = null,
            pageSize = null
        )
    }

    /**
     * Lists pages by layout
     *
     * @param layout layout
     * @return list of pages and count
     */
    suspend fun listByLayout(layout: LayoutEntity): Pair<List<PageEntity>, Long> {
        return applyPagingToQuery(
            query = find("layout = ?1", Sort.ascending("orderNumber"), layout),
            page = null,
            pageSize = null
        )
    }

    /**
     * Creates a new page
     *
     * @param id id
     * @param title title
     * @param survey survey
     * @param layout layout
     * @param orderNumber order number
     * @param nextButtonVisible next button visible
     * @param userId user id
     * @return created page
     */
    suspend fun create(
        id: UUID,
        title: String,
        survey: SurveyEntity,
        layout: LayoutEntity,
        orderNumber: Int,
        nextButtonVisible: Boolean,
        userId: UUID
    ): PageEntity {
        val pageEntity = PageEntity()
        pageEntity.id = id
        pageEntity.title = title
        pageEntity.survey = survey
        pageEntity.layout = layout
        pageEntity.orderNumber = orderNumber
        pageEntity.nextButtonVisible = nextButtonVisible
        pageEntity.creatorId = userId
        pageEntity.lastModifierId = userId
        return persistSuspending(pageEntity)
    }

}
