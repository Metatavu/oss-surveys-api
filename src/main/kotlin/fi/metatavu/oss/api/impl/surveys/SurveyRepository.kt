package fi.metatavu.oss.api.impl.surveys

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import fi.metatavu.oss.api.model.SurveyStatus
import java.util.*
import javax.enterprise.context.ApplicationScoped

/**
 * Repository class for surveys
 */
@ApplicationScoped
class SurveyRepository : AbstractRepository<SurveyEntity, UUID>() {

    /**
     * Creates a survey
     *
     * @param id id
     * @param title title
     * @param creatorId creator id
     * @return uni with created survey
     */
    suspend fun create(id: UUID, title: String, creatorId: UUID): SurveyEntity {
        val surveyEntity = SurveyEntity()
        surveyEntity.id = id
        surveyEntity.title = title
        surveyEntity.status = SurveyStatus.DRAFT
        surveyEntity.creatorId = creatorId
        surveyEntity.lastModifierId = creatorId

        return persistSuspending(surveyEntity)
    }

    /**
     * Updates a survey
     *
     * @param survey survey to update
     * @param title new title
     * @param status new status
     * @param lastModifierId last modifier id
     * @return uni with updated survey
     */
    suspend fun update(
        survey: SurveyEntity,
        title: String,
        status: SurveyStatus,
        lastModifierId: UUID
    ): SurveyEntity {
        survey.title = title
        survey.lastModifierId = lastModifierId
        survey.status = status

        return persistSuspending(survey)
    }
}