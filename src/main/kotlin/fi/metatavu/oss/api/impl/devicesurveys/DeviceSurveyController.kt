package fi.metatavu.oss.api.impl.devicesurveys

import fi.metatavu.oss.api.impl.devices.DeviceEntity
import fi.metatavu.oss.api.impl.pages.PagesController
import fi.metatavu.oss.api.impl.pages.answers.PageAnswerController
import fi.metatavu.oss.api.impl.pages.answers.entities.PageAnswerBaseEntity
import fi.metatavu.oss.api.impl.pages.answers.entities.PageAnswerMulti
import fi.metatavu.oss.api.impl.pages.answers.entities.PageAnswerSingle
import fi.metatavu.oss.api.impl.pages.answers.repositories.MultiAnswersToOptionsRepository
import fi.metatavu.oss.api.impl.pages.questions.PageQuestionController
import fi.metatavu.oss.api.impl.pages.questions.PageQuestionEntity
import fi.metatavu.oss.api.impl.pages.questions.QuestionOptionEntity
import fi.metatavu.oss.api.impl.pages.questions.QuestionOptionRepository
import fi.metatavu.oss.api.impl.realtime.RealtimeNotificationController
import fi.metatavu.oss.api.impl.surveys.SurveyEntity
import fi.metatavu.oss.api.model.*
import io.smallrye.mutiny.coroutines.awaitSuspending
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Controller class for device surveys
 */
@ApplicationScoped
class DeviceSurveyController {

    @Inject
    lateinit var deviceSurveyRepository: DeviceSurveyRepository

    @Inject
    lateinit var realtimeNotificationController: RealtimeNotificationController

    @Inject
    lateinit var answerController: PageAnswerController

    @Inject
    lateinit var pageQuestionController: PageQuestionController

    @Inject
    lateinit var pagesController: PagesController

    @Inject
    lateinit var pageAnswerMultiToOptionsRepository: MultiAnswersToOptionsRepository

    @Inject
    lateinit var questionOptionRepository: QuestionOptionRepository

    /**
     * Lists device surveys by device
     *
     * @param deviceId device id
     * @param firstResult first result
     * @param maxResults max results
     * @return list of device surveys and count
     */
    suspend fun listDeviceSurveysByDevice(
        deviceId: UUID,
        firstResult: Int? = null,
        maxResults: Int? = null,
        status: DeviceSurveyStatus? = null
    ): Pair<List<DeviceSurveyEntity>, Long> {
        return deviceSurveyRepository.list(
            deviceId = deviceId,
            surveyId = null,
            firstResult = firstResult,
            maxResults = maxResults
        )
    }

    /**
     * Lists device surveys by survey
     *
     * @param surveyId survey id
     * @param firstResult first result
     * @param maxResults max results
     * @return list of device surveys and count
     */
    suspend fun listDeviceSurveysBySurvey(
        surveyId: UUID,
        firstResult: Int? = null,
        maxResults: Int? = null,
        status: DeviceSurveyStatus? = null
    ): Pair<List<DeviceSurveyEntity>, Long> {
        return deviceSurveyRepository.list(
            deviceId = null,
            surveyId = surveyId,
            firstResult = firstResult,
            maxResults = maxResults
        )
    }

    /**
     * Finds a device survey
     *
     * @param id device survey id
     * @return found device survey
     */
    suspend fun findDeviceSurvey(id: UUID): DeviceSurveyEntity? {
        return deviceSurveyRepository.findById(id).awaitSuspending()
    }

    /**
     * Creates a device survey
     *
     * @param deviceSurvey device survey to create
     * @param device device
     * @param survey survey
     * @param userId user id
     * @return created device survey
     */
    suspend fun createDeviceSurvey(
        deviceSurvey: DeviceSurvey,
        device: DeviceEntity,
        survey: SurveyEntity,
        userId: UUID
    ): DeviceSurveyEntity {
        val createdDeviceSurvey = deviceSurveyRepository.create(
            device = device,
            survey = survey,
            status = deviceSurvey.status,
            publishStartTime = deviceSurvey.publishStartTime,
            publishEndTime = deviceSurvey.publishEndTime,
            userId = userId
        )

        realtimeNotificationController.notifyDeviceSurveyAction(
            deviceId = device.id,
            deviceSurveyId = createdDeviceSurvey.id,
            action = DeviceSurveysMessageAction.CREATE
        )

        return createdDeviceSurvey
    }

    /**
     * Deletes a device survey
     *
     * @param deviceSurvey device survey to delete
     */
    suspend fun deleteDeviceSurvey(deviceSurvey: DeviceSurveyEntity) {
        deviceSurveyRepository.deleteSuspending(deviceSurvey)

        realtimeNotificationController.notifyDeviceSurveyAction(
            deviceId = deviceSurvey.device.id,
            deviceSurveyId = deviceSurvey.id,
            action = DeviceSurveysMessageAction.DELETE
        )
    }

    /**
     * Updates a device survey
     *
     * @param deviceSurveyToUpdate device survey to update
     * @param newRestDeviceSurvey new device survey data
     * @param userId user id
     * @return updated survey
     */
    suspend fun updateDeviceSurvey(
        deviceSurveyToUpdate: DeviceSurveyEntity,
        newRestDeviceSurvey: DeviceSurvey,
        userId: UUID
    ): DeviceSurveyEntity {
        val updatedDeviceSurvey = deviceSurveyRepository.update(
            deviceSurveyToUpdate,
            status = newRestDeviceSurvey.status,
            publishStartTime = newRestDeviceSurvey.publishStartTime,
            publishEndTime = newRestDeviceSurvey.publishEndTime,
            userId = userId
        )

        realtimeNotificationController.notifyDeviceSurveyAction(
            deviceId = updatedDeviceSurvey.device.id,
            deviceSurveyId = updatedDeviceSurvey.id,
            action = DeviceSurveysMessageAction.UPDATE
        )

        return updatedDeviceSurvey
    }

    /**
     * Notifies devices of updates to associated surveys
     *
     * @param surveyId survey id
     */
    suspend fun notifyDevicesOfSurveyUpdate(surveyId: UUID) {
        val foundDeviceSurveys = deviceSurveyRepository.list(
            deviceId = null,
            surveyId = surveyId,
            firstResult = null,
            maxResults = null
        )

        for (deviceSurvey in foundDeviceSurveys.first) {
            realtimeNotificationController.notifyDeviceSurveyAction(
                deviceId = deviceSurvey.device.id,
                deviceSurveyId = deviceSurvey.id,
                action = DeviceSurveysMessageAction.UPDATE
            )
        }
    }

    /**
     * Validates given scheduled device survey publication times
     *
     * @param deviceSurvey device survey
     * @return whether valid
     */
    fun validateScheduledDeviceSurvey(deviceSurvey: DeviceSurvey): Boolean {
        if (deviceSurvey.publishStartTime == null || deviceSurvey.publishEndTime == null) {
            return false
        }

        if (deviceSurvey.publishEndTime.isBefore(deviceSurvey.publishStartTime)) {
            return false
        }

        if (deviceSurvey.publishStartTime.isBefore(OffsetDateTime.now())) {
            return false
        }

        return true
    }

    /**
     * Lists device surveys that are scheduled to be published
     *
     * @return list of device surveys
     */
    suspend fun listDeviceSurveysToPublish(): List<DeviceSurveyEntity> {
        return deviceSurveyRepository.listDeviceSurveysToPublish()
    }

    /**
     * Publishes a Device Survey
     *
     * @param deviceSurvey device survey to publish
     * @return published device survey
     */
    suspend fun publishDeviceSurvey(deviceSurvey: DeviceSurveyEntity): DeviceSurveyEntity {
        return deviceSurveyRepository.updateStatus(
            deviceSurvey = deviceSurvey,
            status = DeviceSurveyStatus.PUBLISHED,
            publishStartTime = deviceSurvey.publishStartTime,
            publishEndTime = deviceSurvey.publishEndTime
        )
    }

    /**
     * Un-publishes a Device Survey
     *
     * @param deviceSurvey device survey to un-publish
     */
    suspend fun unPublishDeviceSurvey(deviceSurvey: DeviceSurveyEntity) {
        deviceSurveyRepository.deleteSuspending(deviceSurvey)
    }

    /**
     * Returns device survey statistics
     *
     * @param device device
     * @param survey survey
     * @return device survey statistics
     */
    suspend fun getDeviceSurveyStatistics(device: DeviceEntity, survey: SurveyEntity): DeviceSurveyStatistics {
        val answers = answerController.listDeviceSurveyAnswers(
            device = device,
            survey = survey
        )

        return DeviceSurveyStatistics(
            deviceId = device.id,
            surveyId = survey.id,
            totalAnswerCount = answers.size.toLong(),
            averages = calculateDeviceSurveyAverages(answers = answers),
            questions = calculateDeviceSurveyQuestionStatistics(survey = survey, answers = answers)
        )
    }

    /**
     * Calculates device survey averages
     *
     * @param answers device survey answers
     * @return device survey averages
     */
    private suspend fun calculateDeviceSurveyAverages(answers: List<PageAnswerBaseEntity>): DeviceSurveyStatisticsAverages {
        val hourlyCounts: MutableList<Double> = (0..23).map { 0.0 }.toMutableList()
        val weekDayCounts: MutableList<Double> = (0..6).map { 0.0 }.toMutableList()

        if (answers.isEmpty()) {
            return DeviceSurveyStatisticsAverages(
                hourly = hourlyCounts,
                weekDays = weekDayCounts
            )
        }

        for (answer in answers) {
            val createdAt = answer.createdAt?.toInstant()?.atOffset( ZoneOffset.UTC ) ?: continue
            val hour = createdAt.hour
            val dayOfWeek = createdAt.dayOfWeek.value
            hourlyCounts[hour] = hourlyCounts[hour] + 1
            weekDayCounts[dayOfWeek - 1] = weekDayCounts[dayOfWeek - 1] + 1
        }

        return DeviceSurveyStatisticsAverages(
            hourly = hourlyCounts.map { it / answers.size * 100 },
            weekDays = weekDayCounts.map { it / answers.size * 100 }
        )
    }

    /**
     * Calculates device survey question statistics
     *
     * @param survey survey
     * @param answers answers
     * @return list of question statistics
     */
    private suspend fun calculateDeviceSurveyQuestionStatistics(
        survey: SurveyEntity,
        answers: List<PageAnswerBaseEntity>
    ): List<DeviceSurveyQuestionStatistics> {
        val ( pages ) = pagesController.listPages(survey)

        return pages
            .mapNotNull { page ->
                val question = pageQuestionController.find(page) ?: return@mapNotNull null
                val pageAnswers = answers.filter { it.page.id == page.id }

                DeviceSurveyQuestionStatistics(
                    pageId = page.id,
                    pageTitle = page.title,
                    questionType = question.type,
                    options = calculateDeviceSurveyQuestionOptionStatistics(
                        question = question,
                        pageAnswers = pageAnswers
                    ),
                )
            }
    }

    /**
     * Calculates device survey question option statistics
     *
     * @param question question
     * @param pageAnswers page answers
     * @return list of option statistics
     */
    private suspend fun calculateDeviceSurveyQuestionOptionStatistics(
        question: PageQuestionEntity,
        pageAnswers: List<PageAnswerBaseEntity>
    ): List<DeviceSurveyQuestionOptionStatistics> {
        val options = questionOptionRepository.listByQuestion(question)

        return options
            .sortedBy { it.orderNumber }
            .asFlow()
            .map { option ->
                DeviceSurveyQuestionOptionStatistics(
                    questionOptionValue = option.value,
                    answerCount = countAnswersWithOption(answers = pageAnswers, option = option)
                )
            }
            .toList()
    }

    /**
     * Counts answers with given option
     *
     * @param answers answers
     * @param option option
     * @return answer count
     */
    private suspend fun countAnswersWithOption(answers: List<PageAnswerBaseEntity>, option: QuestionOptionEntity): Long {
        return answers.count { answer ->
            listAnswerOptions(answer = answer).any { it.id == option.id }
        }.toLong()
    }

    /**
     * Lists answered options for given answer
     *
     * @param answer answer
     * @return list of answered options
     */
    private suspend fun listAnswerOptions(answer: PageAnswerBaseEntity): List<QuestionOptionEntity> {
        return when (answer) {
            is PageAnswerSingle -> {
                listOf(answer.option)
            }

            is PageAnswerMulti -> {
                pageAnswerMultiToOptionsRepository.listByPageAnswer(answer).map { it.questionOption }
            }

            else -> {
                emptyList()
            }
        }
    }


}