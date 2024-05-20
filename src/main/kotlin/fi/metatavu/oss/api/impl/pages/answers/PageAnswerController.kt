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
import fi.metatavu.oss.api.impl.pages.questions.PageQuestionEntity
import fi.metatavu.oss.api.impl.pages.questions.QuestionOptionEntity
import fi.metatavu.oss.api.impl.pages.questions.QuestionOptionRepository
import fi.metatavu.oss.api.impl.surveys.SurveyEntity
import fi.metatavu.oss.api.model.DevicePageSurveyAnswer
import fi.metatavu.oss.api.model.PageQuestionType.*
import io.smallrye.mutiny.coroutines.awaitSuspending
import org.slf4j.Logger
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
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
        answer: DevicePageSurveyAnswer
    ): PageAnswerBaseEntity {
        val answerKey = createAnswerKey(
            device = device,
            page = page,
            answer = answer
        )

        if (answerKey != null) {
            val existingAnswer = pageAnswerRepository.findByAnswerKey(answerKey)
            if (existingAnswer != null) {
                return existingAnswer
            }
        }

        val answerStringOriginal = answer.answer!!      // was verified to not be empty at the api impl level
        val answerCreatedAt = if (answer.timestamp !== null) {
            OffsetDateTime.ofInstant(Instant.ofEpochSecond(answer.timestamp), ZoneOffset.UTC)
        } else {
            OffsetDateTime.now()
        }
        return when (pageQuestion.type) {
            SINGLE_SELECT -> {
                val option = parseOption(answerStringOriginal)
                    ?: throw IllegalArgumentException("Invalid option id $answerStringOriginal")
                createSingleSelectAnswer(
                    answerKey = answerKey,
                    device = device,
                    page = page,
                    option = option,
                    createdAt = answerCreatedAt
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
                    createdAt = answerCreatedAt
                )
            }

            FREETEXT -> createFreetextAnswer(
                answerKey = answerKey,
                device = device,
                page = page,
                answerStringOriginal = answerStringOriginal,
                createdAt = answerCreatedAt
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

}