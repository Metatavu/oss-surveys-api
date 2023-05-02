package fi.metatavu.oss.api.impl.surveys

import io.smallrye.mutiny.coroutines.awaitSuspending
import java.util.*
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Controller class for surveys (transactions should start here)
 */
@ApplicationScoped
class SurveyController {

    @Inject
    lateinit var surveyRepository: SurveyRepository

    /**
     * Lists surveys
     *
     * @param firstResult first result
     * @param maxResults max results
     * @return uni with list of surveys and count
     */
    suspend fun listSurveys(firstResult: Int?, maxResults: Int?): Pair<List<SurveyEntity>, Long> {
        return surveyRepository.listAllWithPaging(firstResult, maxResults)
    }

    /**
     * Creates a survey
     *
     * @param survey survey to create
     * @param userId user id
     * @return uni with created survey
     */
    suspend fun createSurvey(survey: fi.metatavu.oss.api.model.Survey, userId: UUID): SurveyEntity {
        return surveyRepository.create(
            id = UUID.randomUUID(),
            title = survey.title,
            creatorId = userId
        )
    }

    /**
     * Deletes a survey
     *
     * @param surveyEntity survey to delete
     * @return uni with void
     */
    suspend fun deleteSurvey(surveyEntity: SurveyEntity) {
        surveyRepository.delete(surveyEntity).awaitSuspending()
    }

    /**
     * Updates a survey
     *
     * @param surveyEntityToUpdate survey to update
     * @param newRestSurvey new survey data
     * @param userId user id
     * @return uni with updated survey
     */
    suspend fun updateSurvey(
        surveyEntityToUpdate: SurveyEntity,
        newRestSurvey: fi.metatavu.oss.api.model.Survey,
        userId: UUID
    ): SurveyEntity {
        return surveyRepository.update(surveyEntityToUpdate, newRestSurvey.title, userId)
    }

    /**
     * Finds a survey by id
     *
     * @param surveyId survey id
     * @return uni with found survey
     */
    suspend fun findSurvey(surveyId: UUID): SurveyEntity? {
        return surveyRepository.findById(surveyId).awaitSuspending()
    }
}