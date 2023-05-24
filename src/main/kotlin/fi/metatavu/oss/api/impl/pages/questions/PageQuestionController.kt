package fi.metatavu.oss.api.impl.pages.questions

import fi.metatavu.oss.api.impl.devicesurveys.DeviceSurveyController
import fi.metatavu.oss.api.impl.pages.PageEntity
import fi.metatavu.oss.api.impl.pages.PageRepository
import fi.metatavu.oss.api.impl.pages.answers.PageAnswerController
import fi.metatavu.oss.api.model.PageQuestion
import fi.metatavu.oss.api.model.PageQuestionOption
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
    lateinit var answerController: PageAnswerController

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
     * @param page page
     * @return created question
     */
    suspend fun create(pageQuestion: PageQuestion, page: PageEntity): PageQuestionEntity {
        val question = pageQuestionRepository.create(
            id = UUID.randomUUID(),
            page = page,
            type = pageQuestion.type
        )

        createDependentOptions(pageQuestion.options, question)
        return question
    }

    /**
     * Creates options of the given question
     *
     * @param options options to create from rest objects
     * @param question question to assign options to
     * @return created options
     */
    suspend fun createDependentOptions(options: List<PageQuestionOption>, question: PageQuestionEntity) {
        options.forEach { option ->
            questionOptionRepository.create(
                id = UUID.randomUUID(),
                question = question,
                value = option.questionOptionValue,
                orderNumber = option.orderNumber
            )
        }
    }

    /**
     * Deletes question's options as well as all pages answers.
     *
     * @param question question to delete options from
     */
    suspend fun deleteDependentOptions(question: PageQuestionEntity) {
        answerController.list(question.page).forEach { answer ->
            answerController.delete(answer)
        }
        questionOptionRepository.listByQuestion(question).forEach {
            questionOptionRepository.deleteSuspending(it)
        }
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
        } else if (questionToUpdate == null && newQuestion != null) {
            create(newQuestion, page)
        } else if (questionToUpdate != null && newQuestion == null) {
            delete(questionToUpdate)
        } else if (questionToUpdate != null && newQuestion != null) {
            questionToUpdate.type = newQuestion.type
            pageQuestionRepository.persistSuspending(questionToUpdate)
            deleteDependentOptions(question = questionToUpdate)
            createDependentOptions(options = newQuestion.options, question = questionToUpdate)
        }
    }

    /**
     * Deletes a qustion for the page with all the dependent entities
     *
     * @param pageQuestion entity to delete
     */
    suspend fun delete(pageQuestion: PageQuestionEntity) {
        deleteDependentOptions(pageQuestion)
        pageQuestionRepository.deleteSuspending(pageQuestion)
    }
}