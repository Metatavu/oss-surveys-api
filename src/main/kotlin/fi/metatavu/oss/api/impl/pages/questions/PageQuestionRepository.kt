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
     * @param question question text
     * @param type question type
     * @return created page question entity
     */
    suspend fun create(
        id: UUID,
        page: PageEntity,
        question: String,
        type: PageQuestionType
    ): PageQuestionEntity {
        val pageQuestionEntity = PageQuestionEntity()
        pageQuestionEntity.id = id
        pageQuestionEntity.page = page
        pageQuestionEntity.question = question
        pageQuestionEntity.type = type
        return persistSuspending(pageQuestionEntity)
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