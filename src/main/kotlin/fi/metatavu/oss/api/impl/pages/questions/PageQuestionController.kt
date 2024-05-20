package fi.metatavu.oss.api.impl.pages.questions

import fi.metatavu.oss.api.impl.pages.PageEntity
import fi.metatavu.oss.api.impl.pages.answers.PageAnswerController
import fi.metatavu.oss.api.model.PageQuestion
import fi.metatavu.oss.api.model.PageQuestionOption
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Controller to manage questions of the pages
 */
@ApplicationScoped
class PageQuestionController {

    companion object {
        const val DUMMY_ANSWER_OPTION_VALUE = "Ei valintaa"
    }

    @Inject
    lateinit var pageQuestionRepository: PageQuestionRepository

    @Inject
    lateinit var questionOptionRepository: QuestionOptionRepository

    @Inject
    lateinit var answerController: PageAnswerController

    /**
     * TODO: Add docs
     */
    suspend fun addOption(option: PageQuestionOption, question: PageQuestionEntity): QuestionOptionEntity {
        return questionOptionRepository.create(
            id = UUID.randomUUID(),
            question = question,
            value = option.questionOptionValue,
            orderNumber = option.orderNumber
        )
    }

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
        } else if (questionToUpdate == null && newQuestion != null) {
            create(newQuestion, page)
        } else if (questionToUpdate != null && newQuestion == null) {
            delete(questionToUpdate)
        } else if (questionToUpdate != null && newQuestion != null) {
            val existingOptions = questionOptionRepository.listByQuestion(questionToUpdate)
            val newOptions = newQuestion.options

            pageQuestionRepository.updateType(
                pageQuestion = questionToUpdate,
                type = newQuestion.type
            )

            val removedOptions = existingOptions.filter { existingOption ->
                newOptions.none { newOption ->
                    newOption.id == existingOption.id
                }
            }

            val addedOptions = newOptions.filter { newOption ->
                existingOptions.none { existingOption ->
                    newOption.id == existingOption.id
                }
            }

            val updatedOptions = newOptions.filter { newOption ->
                existingOptions.any { existingOption ->
                    newOption.id == existingOption.id
                }
            }

            removedOptions.forEach { removedOption ->
                questionOptionRepository.deleteSuspending(removedOption)
            }

            addedOptions.forEach { addedOption ->
                questionOptionRepository.create(
                    id = addedOption.id ?: UUID.randomUUID(),
                    question = questionToUpdate,
                    value = addedOption.questionOptionValue,
                    orderNumber = addedOption.orderNumber
                )
            }

            updatedOptions.forEach {
                val questionOption = questionOptionRepository.findById(it.id!!).awaitSuspending()

                questionOptionRepository.updateOrderNumber(
                    questionOption = questionOption,
                    orderNumber = it.orderNumber
                )

                questionOptionRepository.updateValue(
                    questionOption = questionOption,
                    value = it.questionOptionValue
                )
            }
        }
    }

    /**
     * Deletes a question for the page with all the dependent entities
     *
     * @param pageQuestion entity to delete
     */
    suspend fun delete(pageQuestion: PageQuestionEntity) {
        answerController.list(pageQuestion.page).forEach { answer ->
            answerController.delete(answer)
        }

        questionOptionRepository.listByQuestion(pageQuestion).forEach {
            questionOptionRepository.deleteSuspending(it)
        }

        pageQuestionRepository.deleteSuspending(pageQuestion)
    }
}