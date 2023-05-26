package fi.metatavu.oss.api.impl.pages.questions

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import fi.metatavu.oss.api.impl.pages.PageEntity
import fi.metatavu.oss.api.model.PageQuestionType
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.util.UUID
import javax.enterprise.context.ApplicationScoped

/**
 * Repository for Page Questions
 */
@ApplicationScoped
class PageQuestionRepository: AbstractRepository<PageQuestionEntity, UUID>() {

    /**
     * Creates page question
     *
     * @param id id
     * @param page page question will be assigned to
     * @param type question type
     * @return created page question entity
     */
    suspend fun create(
        id: UUID,
        page: PageEntity,
        type: PageQuestionType
    ): PageQuestionEntity {
        val pageQuestionEntity = PageQuestionEntity()
        pageQuestionEntity.id = id
        pageQuestionEntity.page = page
        pageQuestionEntity.type = type
        return persistSuspending(pageQuestionEntity)
    }

    /**
     * Updates page question type
     *
     * @param pageQuestion page question to update
     * @param type new type
     * @return updated page question
     */
    suspend fun updateType(pageQuestion: PageQuestionEntity, type: PageQuestionType): PageQuestionEntity {
        pageQuestion.type = type
        return persistSuspending(pageQuestion)
    }

    /**
     * Finds page question for the given page
     *
     * @param page page
     * @return page question if found or null
     */
    suspend fun findByPage(page: PageEntity): PageQuestionEntity? {
        return find("page", page).firstResult<PageQuestionEntity>().awaitSuspending()
    }

}