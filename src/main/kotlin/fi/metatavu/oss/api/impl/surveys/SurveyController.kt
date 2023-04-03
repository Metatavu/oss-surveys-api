package fi.metatavu.oss.api.impl.surveys

import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional
import io.smallrye.mutiny.Uni
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
     * @return uni with list of surveys and count
     */
    fun listSurveys(firstResult: Int?, maxResults: Int?): Pair<Uni<List<SurveyEntity>>, Uni<Long>> {
        return surveyRepository.listAllWithPaging(firstResult, maxResults)
    }

    /**
     * Creates a survey
     *
     * @param survey survey to create
     * @param userId user id
     * @return uni with created survey
     */
    @ReactiveTransactional
    fun createSurvey(survey: fi.metatavu.oss.api.model.Survey, userId: UUID): Uni<SurveyEntity> {
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
    @ReactiveTransactional
    fun deleteSurvey(surveyEntity: SurveyEntity): Uni<Void> {
        return surveyRepository.delete(surveyEntity)
    }

    /**
     * Updates a survey
     *
     * @param surveyEntityToUpdate survey to update
     * @param newRestSurvey new survey data
     * @param userId user id
     * @return uni with updated survey
     */
    @ReactiveTransactional
    fun updateSurvey(
        surveyEntityToUpdate: SurveyEntity,
        newRestSurvey: fi.metatavu.oss.api.model.Survey,
        userId: UUID
    ): Uni<SurveyEntity> {
        return surveyRepository.update(surveyEntityToUpdate, newRestSurvey.title, userId)
    }

    /**
     * Finds a survey by id
     *
     * @param surveyId survey id
     * @return uni with found survey
     */
    fun findSurvey(surveyId: UUID): Uni<SurveyEntity?> {
        return surveyRepository.findById(surveyId)
    }
}