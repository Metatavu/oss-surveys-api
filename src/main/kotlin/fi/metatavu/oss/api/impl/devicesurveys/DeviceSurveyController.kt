package fi.metatavu.oss.api.impl.devicesurveys

import fi.metatavu.oss.api.impl.devices.DeviceEntity
import fi.metatavu.oss.api.impl.realtime.RealtimeNotificationController
import fi.metatavu.oss.api.impl.surveys.SurveyEntity
import fi.metatavu.oss.api.model.DeviceSurvey
import fi.metatavu.oss.api.model.DeviceSurveyStatus
import fi.metatavu.oss.api.model.DeviceSurveysMessageAction
import io.quarkus.panache.common.Parameters
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.lang.StringBuilder
import java.time.OffsetDateTime
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

    /**
     * Lists device surveys
     *
     * @param deviceId device id
     * @param firstResult first result
     * @param maxResults max results
     * @return list of device surveys and count
     */
    suspend fun listDeviceSurveys(
        deviceId: UUID,
        firstResult: Int?,
        maxResults: Int?,
        status: DeviceSurveyStatus?
    ): Pair<List<DeviceSurveyEntity>, Long> {
        val queryString = StringBuilder()
        queryString.append("device_id = :device_id")
        val parameters = Parameters.with("device_id", deviceId)

        if (status != null) {
            queryString.append(" AND status = :status")
            parameters.and("status", status)
        }

        return deviceSurveyRepository.listWithFilters(
            queryString = queryString.toString(),
            parameters = parameters,
            page = firstResult,
            pageSize = maxResults
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
        val foundDeviceSurveys = deviceSurveyRepository.listWithFilters(
            queryString = "survey_id = :survey_id",
            parameters = Parameters.with("survey_id", surveyId),
            page = null,
            pageSize = null
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
        val queryString = "status = :status AND publishStartTime <= :publishStartTime"
        val parameters = Parameters
            .with("status", DeviceSurveyStatus.SCHEDULED)
            .and("publishStartTime", OffsetDateTime.now())

        return deviceSurveyRepository.listWithFilters(
            queryString = queryString,
            parameters = parameters,
            page = null,
            pageSize = null
        ).first
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
}