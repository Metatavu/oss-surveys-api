package fi.metatavu.oss.api.impl.realtime.mqtt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.smallrye.reactive.messaging.mqtt.MqttMessage
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.reactive.messaging.Emitter
import java.util.*
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

    /**
     * Sends a create message to given device
     *
     * @param deviceId device id
     * @param message message
     */
    abstract fun sendCreateMessage(deviceId: UUID, message: T)

    /**
     * Sends an update message to given device
     *
     * @param deviceId device id
     * @param message message
     */
    abstract fun sendUpdateMessage(deviceId: UUID, message: T)

    /**
     * Sends a delete message to given device
     *
     * @param deviceId device id
     * @param message message
     */
    abstract fun sendDeleteMessage(deviceId: UUID, message: T)

    /**
     * Gets MQTT Topic name based on device id
     */
    protected fun getMqttTopicName(deviceId: UUID): String {
        return "$mqttBaseTopic/$environment/$deviceId"
    }
}