package fi.metatavu.oss.api.impl.pages.answers.repositories

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import fi.metatavu.oss.api.impl.devices.DeviceEntity
import fi.metatavu.oss.api.impl.pages.PageEntity
import fi.metatavu.oss.api.impl.pages.answers.entities.PageAnswerMulti
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
     * @param deviceEntity device entity
     * @param page page
     * @return created PageAnswerMulti entity
     */
    suspend fun create(
        id: UUID,
        deviceEntity: DeviceEntity,
        page: PageEntity
    ): PageAnswerMulti {
        val pageAnswerMulti = PageAnswerMulti()
        pageAnswerMulti.id = id
        pageAnswerMulti.page = page
        pageAnswerMulti.device = deviceEntity
        return persistSuspending(pageAnswerMulti)
    }
}