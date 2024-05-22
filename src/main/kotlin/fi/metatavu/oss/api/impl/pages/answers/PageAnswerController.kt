package fi.metatavu.oss.api.impl.pages.answers

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import fi.metatavu.oss.api.impl.devices.DeviceEntity
import fi.metatavu.oss.api.impl.pages.PageEntity
import fi.metatavu.oss.api.impl.pages.answers.entities.PageAnswerBaseEntity
import fi.metatavu.oss.api.impl.pages.answers.entities.PageAnswerMulti
import fi.metatavu.oss.api.impl.pages.answers.entities.PageAnswerSingle
import fi.metatavu.oss.api.impl.pages.answers.entities.PageAnswerText
import fi.metatavu.oss.api.impl.pages.answers.repositories.*
import fi.metatavu.oss.api.impl.pages.questions.PageQuestionController
import fi.metatavu.oss.api.impl.pages.questions.PageQuestionEntity
import fi.metatavu.oss.api.impl.pages.questions.QuestionOptionEntity
import fi.metatavu.oss.api.impl.pages.questions.QuestionOptionRepository
import fi.metatavu.oss.api.impl.surveys.SurveyEntity
import fi.metatavu.oss.api.model.DevicePageSurveyAnswer
import fi.metatavu.oss.api.model.PageQuestionOption
import fi.metatavu.oss.api.model.PageQuestionType.*
import io.smallrye.mutiny.coroutines.awaitSuspending
import org.slf4j.Logger
import java.time.OffsetDateTime
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Controller for page answers
 */
@ApplicationScoped
class PageAnswerController {

    @Inject
    lateinit var pageOptionRepository: QuestionOptionRepository

    @Inject
    lateinit var pageAnswerMultiRepository: PageAnswerMultiRepository

    @Inject
    lateinit var pageAnswerSingleRepository: PageAnswerSingleRepository

    @Inject
    lateinit var pageAnswerFreetextRepository: PageAnswerTextRepository

    @Inject
    lateinit var pageAnswerMultiToOptionsRepository: MultiAnswersToOptionsRepository

    @Inject
    lateinit var pageAnswerRepository: PageAnswerRepository

    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var objectMapper: ObjectMapper

    @Inject
    lateinit var pageQuestionController: PageQuestionController

    @Inject
    lateinit var questionOptionRepository: QuestionOptionRepository

    /**
     * Lists answers for a page
     *
     * @param page page
     * @return list of answers
     */
    suspend fun list(page: PageEntity): List<PageAnswerBaseEntity> {
        return pageAnswerRepository.listByPage(page)
    }

    /**
     * Lists answers for a device
     *
     * @param device device
     * @return list of answers
     */
    suspend fun list(device: DeviceEntity): List<PageAnswerBaseEntity> {
        return pageAnswerRepository.listByDevice(device)
    }

    /**
     * Creates a new page answer for the page
     *
     * @param device device
     * @param page page
     * @param pageQuestion question that was answered
     * @param answer answer as a string
     * @throws IllegalArgumentException
     * @throws JsonProcessingException
     * @throws JsonMappingException
     */
    suspend fun create(
        device: DeviceEntity,
        page: PageEntity,
        pageQuestion: PageQuestionEntity,
        answer: DevicePageSurveyAnswer,
        createdAt: OffsetDateTime
    ): PageAnswerBaseEntity {
        val answerToSubmit = if (answer.answer.isNullOrEmpty()) {
            getFailsafeQuestionOption(pageQuestion, answer)
        } else {
            answer
        }
        val answerKey = createAnswerKey(
            device = device,
            page = page,
            answer = answerToSubmit
        )

        if (answerKey != null) {
            val existingAnswer = pageAnswerRepository.findByAnswerKey(answerKey)
            if (existingAnswer != null) {
                return existingAnswer
            }
        }

        val answerStringOriginal = answerToSubmit.answer!!
        return when (pageQuestion.type) {
            SINGLE_SELECT -> {
                val option = parseOption(answerStringOriginal)
                    ?: throw IllegalArgumentException("Invalid option id $answerStringOriginal")
                createSingleSelectAnswer(
                    answerKey = answerKey,
                    device = device,
                    page = page,
                    option = option,
                    createdAt = createdAt
                )
            }

            MULTI_SELECT -> {
                val ids = objectMapper.readValue(answerStringOriginal, object : TypeReference<List<String>>() {})
                val options = ids.map {
                    parseOption(it.trim()) ?: throw IllegalArgumentException("Invalid option id $it")
                }
                createMultiSelectAnswer(
                    answerKey = answerKey,
                    device = device,
                    page = page,
                    options = options,
                    createdAt = createdAt
                )
            }

            FREETEXT -> createFreetextAnswer(
                answerKey = answerKey,
                device = device,
                page = page,
                answerStringOriginal = answerStringOriginal,
                createdAt = createdAt
            )
        }
    }

    /**
     * Finds answer by ID
     *
     * @param answerId answer id
     * @param page page
     * @return answer or null if not found
     */
    suspend fun find(
        answerId: UUID,
        page: PageEntity
    ): PageAnswerBaseEntity? {
        return pageAnswerRepository.findByPageAndId(page, answerId)
    }

    /**
     * Deletes an answer
     *
     * @param answer answer to delete
     */
    suspend fun delete(answer: PageAnswerBaseEntity) {
        when (answer) {
            is PageAnswerMulti -> {
                val options = pageAnswerMultiToOptionsRepository.listByPageAnswer(answer)
                for (option in options) {
                    pageAnswerMultiToOptionsRepository.delete(option).awaitSuspending()
                }

                pageAnswerMultiToOptionsRepository.flush().awaitSuspending()
            }
        }

        pageAnswerRepository.deleteSuspending(answer)
    }

    /**
     * Disconnects answer from device. To be used when devices are being removed in order to not lose
     * the submitted answers
     *
     * @param it answer to disconnect
     */
    suspend fun unassignFromDevice(it: PageAnswerBaseEntity) {
        it.device = null
        when (it) {
            is PageAnswerMulti -> pageAnswerMultiRepository.persistSuspending(it)
            is PageAnswerSingle -> pageAnswerSingleRepository.persistSuspending(it)
            is PageAnswerText -> pageAnswerFreetextRepository.persistSuspending(it)
        }
    }

    /**
     * Lists answers for a device survey
     *
     * @param device device
     * @param survey survey
     * @return list of answers
     */
    suspend fun listDeviceSurveyAnswers(device: DeviceEntity, survey: SurveyEntity): List<PageAnswerBaseEntity> {
        return pageAnswerRepository.listByDeviceAndSurvey(
            device = device,
            survey = survey
        )
    }

    /**
     * Creates answer for a freetext question
     *
     * @param answerKey unique key for the answer
     * @param device device where the answer was published
     * @param page page where the answer was published
     * @param answerStringOriginal answer as a string
     * @param createdAt when the answer was created
     * @return created answer
     */
    private suspend fun createFreetextAnswer(
        answerKey: String?,
        device: DeviceEntity,
        page: PageEntity,
        answerStringOriginal: String,
        createdAt: OffsetDateTime
    ): PageAnswerText {
        return pageAnswerFreetextRepository.create(
            id = UUID.randomUUID(),
            answerKey = answerKey,
            page = page,
            deviceEntity = device,
            text = answerStringOriginal,
            createdAt = createdAt
        )
    }

    /**
     * Creates answer to multi select question
     *
     * @param answerKey unique key for the answer
     * @param device device where the answer was published
     * @param page page where the answer was published
     * @param options which options were selected
     * @param createdAt when the answer was created
     * @return created answer
     */
    private suspend fun createMultiSelectAnswer(
        answerKey: String?,
        device: DeviceEntity,
        page: PageEntity,
        options: List<QuestionOptionEntity>,
        createdAt: OffsetDateTime
    ): PageAnswerMulti {
        val answer = pageAnswerMultiRepository.create(
            id = UUID.randomUUID(),
            answerKey = answerKey,
            page = page,
            deviceEntity = device,
            createdAt = createdAt
        )
        options.forEach { answerOption ->
            pageAnswerMultiToOptionsRepository.create(
                id = UUID.randomUUID(),
                pageAnswerMulti = answer,
                option = answerOption
            )
        }

        return answer
    }

    /**
     * Creates a single select answer
     *
     * @param answerKey unique key for the answer
     * @param device where it was published
     * @param page which page it was answered on
     * @param option which option was selected
     * @param createdAt when the answer was created
     * @return created answer
     */
    private suspend fun createSingleSelectAnswer(
        answerKey: String?,
        device: DeviceEntity,
        page: PageEntity,
        option: QuestionOptionEntity,
        createdAt: OffsetDateTime
    ): PageAnswerSingle {
        return pageAnswerSingleRepository.create(
            id = UUID.randomUUID(),
            answerKey = answerKey,
            page = page,
            option = option,
            deviceEntity = device,
            createdAt = createdAt
        )
    }


    /**
     * Finds an option by original ID passed in rest answer object
     *
     * @param id id
     * @return option or null if not found
     */
    private suspend fun parseOption(id: String): QuestionOptionEntity? {
        val uuid = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            logger.error("Invalid uuid $id", e)
            return null
        }
        return pageOptionRepository.findById(uuid).awaitSuspending()
    }

    /**
     * Creates a unique answer key for the answer.
     *
     * If the answer does not have an id, returns null
     *
     * @param device device
     * @param page page
     * @param answer answer
     *
     * @return answer key or null if the answer does not have an id
     */
    private fun createAnswerKey(device: DeviceEntity, page: PageEntity, answer: DevicePageSurveyAnswer): String? {
        val deviceAnswerId = answer.deviceAnswerId ?: return null
        val deviceId = device.id
        val pageId = page.id

        return "$deviceId-$pageId-$deviceAnswerId"
    }

    /**
     * Assigns a failsafe answer value to the answer in case it doesn't have one e.g. the answer is malformed
     *
     * @param pageQuestion question to get failsafe option for
     * @param answer answer submitted from the device with malformed data
     * @return answer with failsafe value
     */
    private suspend fun getFailsafeQuestionOption(pageQuestion: PageQuestionEntity, answer: DevicePageSurveyAnswer): DevicePageSurveyAnswer {
        if (pageQuestion.type == FREETEXT) {
            return answer.copy(answer = "")
        }
        val failsafeAnswer = questionOptionRepository.findFailsafeQuestionOption(pageQuestion.id) ?: createFailsafeQuestionOption(pageQuestion)

        return answer.copy(answer =  when (pageQuestion.type) {
            MULTI_SELECT -> objectMapper.writeValueAsString(listOf(failsafeAnswer.id.toString()))
            SINGLE_SELECT -> failsafeAnswer.id.toString()
            else -> throw IllegalArgumentException("Invalid question type")
        })
    }

    /**
     * Creates a new failsafe failsafe question option for the question
     *
     * @param pageQuestion question to create failsafe option for
     * @return created failsafe option
     */
    private suspend fun createFailsafeQuestionOption(pageQuestion: PageQuestionEntity): QuestionOptionEntity {
        val options = pageOptionRepository.listByQuestion(pageQuestion)
        val maxOrderNumber = options.maxOfOrNull { it.orderNumber ?: 0 } ?: 0

        return pageQuestionController.addOption(
            option = PageQuestionOption(
                questionOptionValue = PageQuestionController.FAILSAFE_NO_SELECTION_OPTION_VALUE,
                orderNumber = maxOrderNumber + 1,
            ),
            question = pageQuestion
        )
    }

}