package fi.metatavu.oss.api.impl.pages.answers.repositories

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import fi.metatavu.oss.api.impl.devices.DeviceEntity
import fi.metatavu.oss.api.impl.devicesurveys.DeviceSurveyEntity
import fi.metatavu.oss.api.impl.pages.PageEntity
import fi.metatavu.oss.api.impl.pages.answers.entities.PageAnswerText
import fi.metatavu.oss.api.impl.surveys.SurveyEntity
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.util.UUID
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
     * @param page page
     * @param deviceEntity device entity
     * @param text text
     * @return created page answer text
     */
    suspend fun create(
        id: UUID,
        page: PageEntity,
        deviceEntity: DeviceEntity,
        text: String
    ): PageAnswerText {
        val pageAnswerText = PageAnswerText()
        pageAnswerText.id = id
        pageAnswerText.page = page
        pageAnswerText.device = deviceEntity
        pageAnswerText.text = text
        return persistSuspending(pageAnswerText)
    }
}