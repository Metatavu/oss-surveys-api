package fi.metatavu.oss.api.impl.realtime.mqtt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.smallrye.reactive.messaging.mqtt.MqttMessage
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.reactive.messaging.Emitter
import javax.inject.Inject

/**
 * Abstract base class for reactive MQTT message producers
 */
abstract class AbstractReactiveProducer<T> {

    @Inject
    @ConfigProperty(name = "mqtt.base.topic")
    protected lateinit var mqttBaseTopic: String

    @Inject
    @ConfigProperty(name = "environment")
    protected lateinit var environment: String

    /**
     * Sends a message to given MQTT topic
     *
     * @param emitter emitter
     * @param topic topic
     * @param message message
     */
    protected fun sendMessage(emitter: Emitter<String>, topic: String, message: T) {
        val mqttMessage = MqttMessage.of(topic,jacksonObjectMapper().writeValueAsString(message))
        emitter.send(mqttMessage)
    }
}