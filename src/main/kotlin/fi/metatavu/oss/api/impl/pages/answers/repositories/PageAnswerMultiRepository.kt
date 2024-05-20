package fi.metatavu.oss.api.impl.pages.answers.repositories

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import fi.metatavu.oss.api.impl.devices.DeviceEntity
import fi.metatavu.oss.api.impl.pages.PageEntity
import fi.metatavu.oss.api.impl.pages.answers.entities.PageAnswerMulti
import java.time.OffsetDateTime
import java.util.*
import javax.enterprise.context.ApplicationScoped

/**
 * Repository for PageAnswerMulti entity
 */
@ApplicationScoped
class PageAnswerMultiRepository : AbstractRepository<PageAnswerMulti, UUID>() {

    /**
     * Creates new PageAnswerMulti entity
     *
     * @param id id
     * @param answerKey unique key for the answer
     * @param deviceEntity device entity
     * @param page page
     * @param createdAt creation time
     * @return created PageAnswerMulti entity
     */
    suspend fun create(
        id: UUID,
        answerKey: String?,
        deviceEntity: DeviceEntity,
        page: PageEntity,
        createdAt: OffsetDateTime
    ): PageAnswerMulti {
        val pageAnswerMulti = PageAnswerMulti()
        pageAnswerMulti.id = id
        pageAnswerMulti.answerKey = answerKey
        pageAnswerMulti.page = page
        pageAnswerMulti.device = deviceEntity
        pageAnswerMulti.createdAt = createdAt
        pageAnswerMulti.modifiedAt = createdAt
        return persistSuspending(pageAnswerMulti)
    }
}