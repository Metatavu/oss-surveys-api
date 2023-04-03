package fi.metatavu.oss.api.impl.surveys

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import io.smallrye.mutiny.Uni
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
    fun create(id: UUID, title: String, creatorId: UUID): Uni<SurveyEntity> {
        val surveyEntity = SurveyEntity()
        surveyEntity.id = id
        surveyEntity.title = title
        surveyEntity.creatorId = creatorId
        surveyEntity.lastModifierId = creatorId
        return persist(surveyEntity)
    }

    /**
     * Updates a survey
     *
     * @param survey survey to update
     * @param title new title
     * @param lastModifierId last modifier id
     * @return uni with updated survey
     */
    fun update(survey: SurveyEntity, title: String, lastModifierId: UUID): Uni<SurveyEntity> {
        survey.title = title
        survey.lastModifierId = lastModifierId
        return persist(survey)
    }
}