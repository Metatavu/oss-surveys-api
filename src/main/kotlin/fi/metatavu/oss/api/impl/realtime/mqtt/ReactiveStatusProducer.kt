package fi.metatavu.oss.api.impl.realtime.mqtt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.smallrye.reactive.messaging.mqtt.MqttMessage
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class ReactiveStatusProducer(
    @Channel(CHANNEL_NAME) private val emitter: Emitter<String>
) {

    fun sendStatusMessage(message: DeviceStatus) {
        emitter.send(MqttMessage.of("oss/staging/123/status", jacksonObjectMapper().writeValueAsString(message)))
    }
    companion object {
        private const val CHANNEL_NAME = "test"
    }
}