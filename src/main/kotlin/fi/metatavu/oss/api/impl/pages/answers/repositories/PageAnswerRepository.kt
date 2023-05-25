package fi.metatavu.oss.api.impl.pages.answers.repositories

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import fi.metatavu.oss.api.impl.devices.DeviceEntity
import fi.metatavu.oss.api.impl.pages.PageEntity
import fi.metatavu.oss.api.impl.pages.answers.entities.PageAnswerBaseEntity
import fi.metatavu.oss.api.impl.surveys.SurveyEntity
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.util.*
import javax.enterprise.context.ApplicationScoped

/**
 * Repository for based PageAnswer entity
 */
@ApplicationScoped
class PageAnswerRepository : AbstractRepository<PageAnswerBaseEntity, UUID>() {

    /**
     * Lists PageAnswer entities by page
     *
     * @param page page
     * @return list of PageAnswer entities
     */
    suspend fun listByPage(page: PageEntity): List<PageAnswerBaseEntity> {
        return list("page", page).awaitSuspending()
    }

    /**
     * Lists PageAnswer entities by device
     *
     * @param device device
     * @return list of PageAnswer entities
     */
    suspend fun listByDevice(device: DeviceEntity): List<PageAnswerBaseEntity> {
        return list("device = ?1", device).awaitSuspending()

    }

    /**
     * Finds PageAnswer by page and id
     *
     * @param page page
     * @param answerId answer id
     * @return found PageAnswer or null if not found
     */
    suspend fun findByPageAndId(page: PageEntity, answerId: UUID): PageAnswerBaseEntity? {
        return find("page = ?1 and id = ?2", page, answerId).firstResult<PageAnswerBaseEntity?>().awaitSuspending()
    }

    /**
     * Lists answers by device and survey
     *
     * @param device device
     * @param survey survey
     * @return list of answers
     */
    suspend fun listByDeviceAndSurvey(device: DeviceEntity, survey: SurveyEntity): List<PageAnswerBaseEntity> {
        return list("device = ?1 and page.survey = ?2", device, survey).awaitSuspending()
    }

}