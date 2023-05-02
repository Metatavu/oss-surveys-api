package fi.metatavu.oss.api.impl.surveys

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.util.*
import jakarta.enterprise.context.ApplicationScoped

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
        surveyEntity.creatorId = creatorId
        surveyEntity.lastModifierId = creatorId
        return persist(surveyEntity).awaitSuspending()
    }

    /**
     * Updates a survey
     *
     * @param survey survey to update
     * @param title new title
     * @param lastModifierId last modifier id
     * @return uni with updated survey
     */
    suspend fun update(survey: SurveyEntity, title: String, lastModifierId: UUID): SurveyEntity {
        survey.title = title
        survey.lastModifierId = lastModifierId
        return persist(survey).awaitSuspending()
    }
}