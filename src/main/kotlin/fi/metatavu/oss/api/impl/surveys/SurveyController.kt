package fi.metatavu.oss.api.impl.surveys

import fi.metatavu.oss.api.model.SurveyStatus
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

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
     * @param status status
     * @return list of surveys and count
     */
    suspend fun listSurveys(
        firstResult: Int?,
        maxResults: Int?,
        status: SurveyStatus?
    ): Pair<List<SurveyEntity>, Long> {
        if (status != null) {
            return surveyRepository.applyPagingToQuery(
                query = surveyRepository.find("status = ?1", status),
                page = firstResult,
                pageSize = maxResults,
            )
        }

        return surveyRepository.listAllWithPaging(firstResult, maxResults)
    }

    /**
     * Creates a survey
     *
     * @param survey survey to create
     * @param userId user id
     * @return created survey
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
     */
    suspend fun deleteSurvey(surveyEntity: SurveyEntity) {
        surveyRepository.deleteSuspending(surveyEntity)
    }

    /**
     * Updates a survey
     *
     * @param surveyEntityToUpdate survey to update
     * @param newRestSurvey new survey data
     * @param userId user id
     * @return updated survey
     */
    suspend fun updateSurvey(
        surveyEntityToUpdate: SurveyEntity,
        newRestSurvey: fi.metatavu.oss.api.model.Survey,
        userId: UUID
    ): SurveyEntity {
        return surveyRepository.update(
            survey = surveyEntityToUpdate,
            title = newRestSurvey.title,
            status  = newRestSurvey.status ?: SurveyStatus.DRAFT,
            lastModifierId = userId
        )
    }

    /**
     * Finds a survey by id
     *
     * @param surveyId survey id
     * @return found survey
     */
    suspend fun findSurvey(surveyId: UUID): SurveyEntity? {
        return surveyRepository.findById(surveyId).awaitSuspending()
    }
}