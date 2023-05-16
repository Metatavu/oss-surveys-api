package fi.metatavu.oss.api.impl.pages.questions

import fi.metatavu.oss.api.impl.devicesurveys.DeviceSurveyController
import fi.metatavu.oss.api.impl.pages.PageEntity
import fi.metatavu.oss.api.model.DeviceSurveyStatus
import fi.metatavu.oss.api.model.PageQuestion
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Controller to manage questions of the pages
 */
@ApplicationScoped
class PageQuestionController {

    @Inject
    lateinit var pageQuestionRepository: PageQuestionRepository

    @Inject
    lateinit var questionOptionRepository: QuestionOptionRepository

    @Inject
    lateinit var deviceSurveyController: DeviceSurveyController

    /**
     * Finds questions assigned to the page if any
     *
     * @param page page
     * @return question or null
     */
    suspend fun find(page: PageEntity): PageQuestionEntity? {
        return pageQuestionRepository.findByPage(page)
    }

    /**
     * Creates a new question for the page
     *
     * @param id id
     * @param page page
     * @param question question
     * @param type type
     * @return created question
     */
    suspend fun create(pageQuestion: PageQuestion, page: PageEntity): PageQuestionEntity {
        val question = pageQuestionRepository.create(
            id = UUID.randomUUID(),
            page = page,
            question = pageQuestion.question,
            type = pageQuestion.type
        )

        pageQuestion.options.forEach { option ->
            questionOptionRepository.create(
                id = UUID.randomUUID(),
                question = question,
                value = option.questionOptionValue,
                orderNumber = option.orderNumber
            )
        }

        return question
    }

    /**
     * Updates page's question.
     * In case the page is already published in a survey, the answer options are not updatable.
     * In case it is not published yet, andwer options can be updated.
     * Re-assigning page is not allowed.
     *
     * @param questionToUpdate JPA entity to update
     * @param newQuestion REST object for new question data
     * @param page JPA page the question belongs to
     */
    suspend fun update(
        questionToUpdate: PageQuestionEntity?,
        newQuestion: PageQuestion?,
        page: PageEntity
    ) {
        if (questionToUpdate == null && newQuestion == null) {
            return
        }

        if (questionToUpdate == null) {
            if (newQuestion != null) {
                pageQuestionRepository.create(
                    id = UUID.randomUUID(),
                    page = page,
                    question = newQuestion.question,
                    type = newQuestion.type
                )
            }
        } else if (newQuestion == null) {
            pageQuestionRepository.deleteSuspending(questionToUpdate)
        } else {
            questionToUpdate.question = newQuestion.question
            questionToUpdate.type = newQuestion.type
            pageQuestionRepository.persistSuspending(questionToUpdate)

            /* Do not update answer options on the published survey because it will mess up already submitted answers */
            val deviceSurvey = deviceSurveyController.listDeviceSurveysBySurvey(surveyId = page.survey.id)
            if (deviceSurvey.first.isNotEmpty() && deviceSurvey.first.find { it.status == DeviceSurveyStatus.PUBLISHED } != null) {
                return
            }

            // Update answer options
            val existingOptions = questionOptionRepository.listByQuestion(questionToUpdate)
            val newOptions = newQuestion.options
            existingOptions.forEach {
                questionOptionRepository.deleteSuspending(it)
            }
            newOptions.forEach { option ->
                questionOptionRepository.create(
                    id = UUID.randomUUID(),
                    question = questionToUpdate,
                    value = option.questionOptionValue,
                    orderNumber = option.orderNumber
                )
            }
        }
    }

    /**
     * Deletes a qustion for the page with all the dependent entities
     *
     * @param pageQuestion entity to delete
     */
    suspend fun delete(pageQuestion: PageQuestionEntity) {
        questionOptionRepository.listByQuestion(pageQuestion).forEach {
            questionOptionRepository.deleteSuspending(it)
        }
        pageQuestionRepository.deleteSuspending(pageQuestion)
    }
}