package fi.metatavu.oss.api.impl.realtime.mqtt

import fi.metatavu.oss.api.model.DeviceSurveyMessage
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Channel
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Reactive MQTT producer for Device Survey messages
 */
@ApplicationScoped
class ReactiveDeviceSurveysProducer: AbstractReactiveDeviceProducer<DeviceSurveyMessage>() {

    @Inject
    @Channel(CHANNEL_NAME)
    lateinit var emitter: Emitter<String>

    override fun sendCreateMessage(deviceId: UUID, message: DeviceSurveyMessage) {
        val topic = getMqttTopicName(deviceId) + "/surveys/create"
        super.sendMessage(
            emitter = emitter,
            topic = topic,
            message = message
        )
    }

    override fun sendUpdateMessage(deviceId: UUID, message: DeviceSurveyMessage) {
        val topic = getMqttTopicName(deviceId) + "/surveys/update"
        super.sendMessage(
            emitter = emitter,
            topic = topic,
            message = message
        )
    }

    override fun sendDeleteMessage(deviceId: UUID, message: DeviceSurveyMessage) {
        val topic = getMqttTopicName(deviceId) + "/surveys/delete"
        super.sendMessage(
            emitter = emitter,
            topic = topic,
            message = message
        )
    }

    companion object{
        const val CHANNEL_NAME = "surveys"
    }
}