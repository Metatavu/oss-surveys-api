package fi.metatavu.oss.api.impl.pages.answers

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import fi.metatavu.oss.api.impl.devices.DeviceEntity
import fi.metatavu.oss.api.impl.devicesurveys.DeviceSurveyEntity
import fi.metatavu.oss.api.impl.pages.PageEntity
import fi.metatavu.oss.api.impl.pages.answers.entities.PageAnswerBaseEntity
import fi.metatavu.oss.api.impl.pages.answers.entities.PageAnswerMulti
import fi.metatavu.oss.api.impl.pages.answers.entities.PageAnswerSingle
import fi.metatavu.oss.api.impl.pages.answers.entities.PageAnswerText
import fi.metatavu.oss.api.impl.pages.answers.repositories.*
import fi.metatavu.oss.api.impl.pages.questions.PageQuestionEntity
import fi.metatavu.oss.api.impl.pages.questions.QuestionOptionEntity
import fi.metatavu.oss.api.impl.pages.questions.QuestionOptionRepository
import fi.metatavu.oss.api.model.DevicePageSurveyAnswer
import fi.metatavu.oss.api.model.PageQuestionType.*
import io.smallrye.mutiny.coroutines.awaitSuspending
import org.slf4j.Logger
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
     * @param deviceSurvey device survey
     * @param page page
     * @param pageQuestion question that was answered
     * @param answer answer as a string
     * @throws IllegalArgumentException
     * @throws JsonProcessingException
     * @throws JsonMappingException
     */
    suspend fun create(
        deviceSurvey: DeviceSurveyEntity,
        page: PageEntity,
        pageQuestion: PageQuestionEntity,
        answer: DevicePageSurveyAnswer
    ): PageAnswerBaseEntity {
        val answerStringOriginal = answer.answer!!      // was verified to not be empty at the api impl level
        return when (pageQuestion.type) {
            SINGLE_SELECT -> {
                val option = parseOption(answerStringOriginal)
                    ?: throw IllegalArgumentException("Invalid option id $answerStringOriginal")
                createSingleSelectAnswer(deviceSurvey, page, option)
            }

            MULTI_SELECT -> {
                val ids = objectMapper.readValue(answerStringOriginal, object : TypeReference<List<String>>() {})
                val options = ids.map {
                    parseOption(it.trim()) ?: throw IllegalArgumentException("Invalid option id $it")
                }
                createMultiSelectAnswer(deviceSurvey, page, options)
            }

            FREETEXT -> createFreetextAnswer(deviceSurvey, page, answerStringOriginal)
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
     * @param deviceSurvey device survey where the answer was published
     * @param page page where the answer was published
     * @param answerStringOriginal answer as a string
     * @return created answer
     */
    private suspend fun createFreetextAnswer(
        deviceSurvey: DeviceSurveyEntity,
        page: PageEntity,
        answerStringOriginal: String
    ): PageAnswerText {
        return pageAnswerFreetextRepository.create(
            id = UUID.randomUUID(),
            page = page,
            deviceEntity = deviceSurvey.device,
            text = answerStringOriginal
        )
    }

    /**
     * Creates answer to multi select question
     *
     * @param deviceSurvey device survey where the answer was published
     * @param page page where the answer was published
     * @param options which options were selected
     * @return created answer
     */
    private suspend fun createMultiSelectAnswer(
        deviceSurvey: DeviceSurveyEntity,
        page: PageEntity,
        options: List<QuestionOptionEntity>
    ): PageAnswerMulti {
        val answer = pageAnswerMultiRepository.create(
            id = UUID.randomUUID(),
            page = page,
            deviceEntity = deviceSurvey.device
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
     * @param deviceSurvey where it was published
     * @param page which page it was answered on
     * @param option which option was selected
     * @return created answer
     */
    private suspend fun createSingleSelectAnswer(
        deviceSurvey: DeviceSurveyEntity,
        page: PageEntity,
        option: QuestionOptionEntity
    ): PageAnswerSingle {
        return pageAnswerSingleRepository.create(
            id = UUID.randomUUID(),
            page = page,
            option = option,
            deviceEntity = deviceSurvey.device
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
     * @param deviceSurvey device survey
     * @return list of answers
     */
    suspend fun listDeviceSurveyAnswers(deviceSurvey: DeviceSurveyEntity): List<PageAnswerBaseEntity> {
        return pageAnswerRepository.listByDeviceAndSurvey(
            device = deviceSurvey.device,
            survey = deviceSurvey.survey
        )
    }

}