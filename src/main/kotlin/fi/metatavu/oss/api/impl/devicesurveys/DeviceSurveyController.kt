package fi.metatavu.oss.api.impl.devicesurveys

import fi.metatavu.oss.api.impl.devices.DeviceEntity
import fi.metatavu.oss.api.impl.surveys.SurveyEntity
import fi.metatavu.oss.api.model.DeviceSurvey
import io.smallrye.mutiny.coroutines.awaitSuspending
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
        maxResults: Int?
    ): Pair<List<DeviceSurveyEntity>, Long> {
        return deviceSurveyRepository.applyPagingToQuery(
            query = deviceSurveyRepository.find("device_id = ?1", deviceId),
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
        return deviceSurveyRepository.create(
            device = device,
            survey = survey,
            status = deviceSurvey.status,
            publishStartTime = deviceSurvey.publishStartTime,
            publishEndTime = deviceSurvey.publishEndTime,
            userId = userId
        )
    }

    /**
     * Deletes a device survey
     *
     * @param deviceSurvey device survey to delete
     */
    suspend fun deleteDeviceSurvey(deviceSurvey: DeviceSurveyEntity) {
        deviceSurveyRepository.deleteSuspending(deviceSurvey)
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
        return deviceSurveyRepository.update(
            deviceSurveyToUpdate,
            status = newRestDeviceSurvey.status,
            publishStartTime = newRestDeviceSurvey.publishStartTime,
            publishEndTime = newRestDeviceSurvey.publishEndTime,
            userId = userId
        )
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
}