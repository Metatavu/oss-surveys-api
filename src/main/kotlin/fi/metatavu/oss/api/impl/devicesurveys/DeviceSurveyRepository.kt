package fi.metatavu.oss.api.impl.devicesurveys

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import fi.metatavu.oss.api.impl.devices.DeviceEntity
import fi.metatavu.oss.api.impl.surveys.SurveyEntity
import fi.metatavu.oss.api.model.DeviceSurveyStatus
import io.quarkus.panache.common.Parameters
import java.lang.StringBuilder
import java.time.OffsetDateTime
import java.util.UUID
import javax.enterprise.context.ApplicationScoped

/**
 * Repository class for device surveys
 */
@ApplicationScoped
class DeviceSurveyRepository: AbstractRepository<DeviceSurveyEntity, UUID>() {

    /**
     * Lists device surveys
     *
     * @param deviceId device id
     * @param firstResult first result
     * @param maxResults max results
     * @return list of device surveys and count
     */
    suspend fun list(
        deviceId: UUID?,
        surveyId: UUID?,
        firstResult: Int? = null,
        maxResults: Int? = null,
        status: DeviceSurveyStatus? = null
    ): Pair<List<DeviceSurveyEntity>, Long> {
        val queryString = StringBuilder()
        val parameters = Parameters()

        if (deviceId != null) {
            queryString.append("device_id = :device_id")
            parameters.and("device_id", deviceId)
        }

        if (surveyId != null) {
            queryString.append("survey_id = :survey_id")
            parameters.and("survey_id", surveyId)
        }

        if (status != null) {
            queryString.append(" AND status = :status")
            parameters.and("status", status)
        }

        return listWithFilters(
            queryString = queryString.toString(),
            parameters = parameters,
            page = firstResult,
            pageSize = maxResults
        )
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

        return listWithFilters(
            queryString = queryString,
            parameters = parameters,
            page = null,
            pageSize = null
        ).first
    }

    /**
     * Creates a Device Survey
     *
     * @param device device
     * @param survey survey
     * @param status status
     * @param publishStartTime publication start time
     * @param publishEndTime publication end time
     * @param userId user id
     * @return created device survey
     */
    suspend fun create(
        device: DeviceEntity,
        survey: SurveyEntity,
        status: DeviceSurveyStatus,
        publishStartTime: OffsetDateTime?,
        publishEndTime: OffsetDateTime?,
        userId: UUID
    ): DeviceSurveyEntity {
        val deviceSurveyEntity = DeviceSurveyEntity()
        deviceSurveyEntity.id = UUID.randomUUID()
        deviceSurveyEntity.device = device
        deviceSurveyEntity.survey = survey
        deviceSurveyEntity.status = status
        deviceSurveyEntity.publishStartTime = publishStartTime
        deviceSurveyEntity.publishEndTime = publishEndTime
        deviceSurveyEntity.creatorId = userId
        deviceSurveyEntity.lastModifierId = userId

        return persistSuspending(deviceSurveyEntity)
    }

    /**
     * Updates a Device Survey
     *
     * @param status status
     * @param publishStartTime publication start time
     * @param publishEndTime publication end time
     * @param userId user id
     * @return updated device survey
     */
    suspend fun update(
        deviceSurveyEntity: DeviceSurveyEntity,
        status: DeviceSurveyStatus,
        publishStartTime: OffsetDateTime?,
        publishEndTime: OffsetDateTime?,
        userId: UUID
    ): DeviceSurveyEntity {
        deviceSurveyEntity.status = status
        deviceSurveyEntity.publishStartTime = publishStartTime
        deviceSurveyEntity.publishEndTime = publishEndTime
        deviceSurveyEntity.lastModifierId = userId

        return persistSuspending(deviceSurveyEntity)
    }

    /**
     * Changes status of Device Survey
     *
     * @param deviceSurvey device survey
     * @param status status
     * @param publishStartTime publication start time
     * @param publishEndTime publication end time
     */
    suspend fun updateStatus(
        deviceSurvey: DeviceSurveyEntity,
        status: DeviceSurveyStatus,
        publishStartTime: OffsetDateTime?,
        publishEndTime: OffsetDateTime?
    ): DeviceSurveyEntity {
        deviceSurvey.status = status
        deviceSurvey.publishStartTime = publishStartTime
        deviceSurvey.publishEndTime = publishEndTime

        return persistSuspending(deviceSurvey)
    }
}