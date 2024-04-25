package fi.metatavu.oss.api.impl.pages.answers.repositories

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import fi.metatavu.oss.api.impl.devices.DeviceEntity
import fi.metatavu.oss.api.impl.pages.PageEntity
import fi.metatavu.oss.api.impl.pages.answers.entities.PageAnswerText
import java.util.*
import javax.enterprise.context.ApplicationScoped

/**
 * Repository for PageAnswerText entity
 */
@ApplicationScoped
class PageAnswerTextRepository: AbstractRepository<PageAnswerText, UUID>() {

    /**
     * Creates page answer text
     *
     * @param id id
     * @param answerKey unique key for the answer
     * @param page page
     * @param deviceEntity device entity
     * @param text text
     * @return created page answer text
     */
    suspend fun create(
        id: UUID,
        answerKey: String?,
        page: PageEntity,
        deviceEntity: DeviceEntity,
        text: String
    ): PageAnswerText {
        val pageAnswerText = PageAnswerText()
        pageAnswerText.id = id
        pageAnswerText.answerKey = answerKey
        pageAnswerText.page = page
        pageAnswerText.device = deviceEntity
        pageAnswerText.text = text
        return persistSuspending(pageAnswerText)
    }
}