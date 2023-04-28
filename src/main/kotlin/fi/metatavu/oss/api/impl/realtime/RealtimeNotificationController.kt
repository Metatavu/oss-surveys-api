package fi.metatavu.oss.api.impl.realtime

import fi.metatavu.oss.api.impl.realtime.mqtt.ReactiveDeviceSurveysProducer
import fi.metatavu.oss.api.model.DeviceSurveyMessage
import fi.metatavu.oss.api.model.DeviceSurveysMessageAction
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
     * Sends a notification to a device that given action has been performed for given device survey
     *
     * @param deviceId device id
     * @param deviceSurveyId device survey id
     * @param action action
     */
    fun notifyDeviceSurveyAction(
        deviceId: UUID,
        deviceSurveyId: UUID,
        action: DeviceSurveysMessageAction
    ) {
        val deviceSurveyMessage = DeviceSurveyMessage(
            deviceId = deviceId,
            deviceSurveyId = deviceSurveyId,
            action = action
        )
        when (action) {
            DeviceSurveysMessageAction.CREATE -> {
                deviceSurveysProducer.sendCreateMessage(
                    deviceId = deviceId,
                    message = deviceSurveyMessage
                )
            }
            DeviceSurveysMessageAction.UPDATE -> {
                deviceSurveysProducer.sendUpdateMessage(
                    deviceId = deviceId,
                    message = deviceSurveyMessage
                )
            }
            DeviceSurveysMessageAction.DELETE -> {
                deviceSurveysProducer.sendDeleteMessage(
                    deviceId = deviceId,
                    message = deviceSurveyMessage
                )
            }
        }
    }
}