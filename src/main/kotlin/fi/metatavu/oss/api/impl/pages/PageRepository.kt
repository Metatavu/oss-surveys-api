package fi.metatavu.oss.api.impl.pages

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import fi.metatavu.oss.api.impl.surveys.SurveyEntity
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import java.util.*

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
    suspend fun listBySurvey(survey: SurveyEntity): Pair<List<PageEntity>, Int> {
        val pages = list("survey = ?1", survey).awaitSuspending()
        return Pair(pages, pages.size)
    }

    /**
     * Creates a new page
     *
     * @param id id
     * @param title title
     * @param html html
     * @param survey survey
     * @param userId user id
     * @return created page
     */
    suspend fun create(id: UUID, title: String, html: String, survey: SurveyEntity, userId: UUID): PageEntity {
        val pageEntity = PageEntity()
        pageEntity.id = id
        pageEntity.title = title
        pageEntity.html = html
        pageEntity.survey = survey
        pageEntity.creatorId = userId
        pageEntity.lastModifierId = userId
        return persistSuspending(pageEntity)
    }

}
