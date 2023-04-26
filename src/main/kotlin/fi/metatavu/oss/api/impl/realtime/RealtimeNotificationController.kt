package fi.metatavu.oss.api.impl.realtime

import fi.metatavu.oss.api.impl.realtime.mqtt.ReactiveDeviceSurveysProducer
import fi.metatavu.oss.api.model.DeviceSurveyMessage
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Controller for realtime notifications
 */
@ApplicationScoped
class RealtimeNotificationController {

    @Inject
    private lateinit var deviceSurveysProducer: ReactiveDeviceSurveysProducer

    /**
     * Sends a notification to a device that a new survey has been created
     *
     * @param deviceId device id
     * @param surveyId survey id
     */
    fun notifyDeviceSurveyCreated(deviceId: UUID, surveyId: UUID) {
        deviceSurveysProducer.sendCreateMessage(
            deviceId = deviceId,
            message = DeviceSurveyMessage(
                deviceId = deviceId,
                surveyId = surveyId
            )
        )
    }

    /**
     * Sends a notification to a device that an existing survey has been updated
     *
     * @param deviceId device id
     * @param surveyId survey id
     */
    fun notifyDeviceSurveyUpdated(deviceId: UUID, surveyId: UUID) {
        deviceSurveysProducer.sendCreateMessage(
            deviceId = deviceId,
            message = DeviceSurveyMessage(
                deviceId = deviceId,
                surveyId = surveyId
            )
        )
    }

    /**
     * Sends a notification to a device that an existing survey has been deleted
     *
     * @param deviceId device id
     * @param surveyId survey id
     */
    fun notifyDeviceSurveyDeleted(deviceId: UUID, surveyId: UUID) {
        deviceSurveysProducer.sendCreateMessage(
            deviceId = deviceId,
            message = DeviceSurveyMessage(
                deviceId = deviceId,
                surveyId = surveyId
            )
        )
    }
}