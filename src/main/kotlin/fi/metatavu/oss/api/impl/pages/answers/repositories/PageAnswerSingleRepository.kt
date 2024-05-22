package fi.metatavu.oss.api.impl.pages.answers.repositories

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import fi.metatavu.oss.api.impl.devices.DeviceEntity
import fi.metatavu.oss.api.impl.pages.PageEntity
import fi.metatavu.oss.api.impl.pages.answers.entities.PageAnswerSingle
import fi.metatavu.oss.api.impl.pages.questions.QuestionOptionEntity
import java.time.OffsetDateTime
import java.util.*
import javax.enterprise.context.ApplicationScoped

/**
 * Repository for PageAnswerSingle entity
 */
@ApplicationScoped
class PageAnswerSingleRepository : AbstractRepository<PageAnswerSingle, UUID>() {

    /**
     * Creates page answer single
     *
     * @param id id
     * @param answerKey unique key for the answer
     * @param page page
     * @param deviceEntity device entity
     * @param option option
     * @param createdAt creation time
     * @return created page answer single
     */
    suspend fun create(
        id: UUID,
        answerKey: String?,
        page: PageEntity,
        deviceEntity: DeviceEntity,
        option: QuestionOptionEntity,
        createdAt: OffsetDateTime
    ): PageAnswerSingle {
        val pageAnswerSingle = PageAnswerSingle()
        pageAnswerSingle.id = id
        pageAnswerSingle.answerKey = answerKey
        pageAnswerSingle.page = page
        pageAnswerSingle.device = deviceEntity
        pageAnswerSingle.option = option
        pageAnswerSingle.createdAt = createdAt
        pageAnswerSingle.modifiedAt = createdAt
        return persistSuspending(pageAnswerSingle)
    }

}