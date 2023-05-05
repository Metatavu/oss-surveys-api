package fi.metatavu.oss.api.impl.surveys

import fi.metatavu.oss.api.model.SurveyStatus
import io.quarkus.panache.common.Parameters
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
     * @param rangeStart page
     * @param rangeEnd pageSize
     * @param status status
     * @return list of surveys and count
     */
    suspend fun listSurveys(
        rangeStart: Int?,
        rangeEnd: Int?,
        status: SurveyStatus?
    ): Pair<List<SurveyEntity>, Long> {
        val stringBuilder = StringBuilder()
        val parameters = Parameters()

        if (status != null) {
            stringBuilder.append("status = :status")
            parameters.and("status", status)
        }

        return surveyRepository.applyRangeToQuery(
            query = surveyRepository.find(stringBuilder.toString(), parameters),
            firstIndex = rangeStart,
            lastIndex = rangeEnd
        )
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
            description = survey.description,
            timeout = survey.timeout,
            status = SurveyStatus.DRAFT,
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
            description = newRestSurvey.description,
            timeout = newRestSurvey.timeout,
            status  = newRestSurvey.status,
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