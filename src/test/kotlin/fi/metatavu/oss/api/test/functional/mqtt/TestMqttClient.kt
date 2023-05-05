package fi.metatavu.oss.api.test.functional.mqtt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.eclipse.microprofile.config.ConfigProvider
import org.eclipse.paho.client.mqttv3.*
import java.util.*

/**
 * MQTT Client for functional tests
 */
class TestMqttClient: AutoCloseable {

    private val subscriptions = mutableMapOf<String, MutableList<TestMqttSubscription<*>>>()
    private val baseTopic = ConfigProvider.getConfig().getValue("mqtt.base.topic", String::class.java)
    private var mqttClient: MqttClient? = null

    /**
     * Constructor. Connects to MQTT server
     */
    init {
        val client = MqttClient(
            "tcp://" + ConfigProvider.getConfig().getValue("mqtt.server.url", String::class.java),
            UUID.randomUUID().toString()
        )
        val options = MqttConnectOptions()
        options.isAutomaticReconnect = true
        options.isCleanSession = true
        options.connectionTimeout = 10

        client.connect(options)
        mqttClient = client
    }

    /**
     * Subscribes to MQTT topic
     *
     * @param targetClass message class
     * @param subscriptionTopic topic to subscribe
     */
    fun <T> subscribe(targetClass: Class<T>, subscriptionTopic: String): TestMqttSubscription<T> {
        val topic = "${baseTopic}/test/$subscriptionTopic"
        var topicSubscription = subscriptions[topic]
        if (topicSubscription == null) {
            topicSubscription = mutableListOf()
            subscriptions[topic] = topicSubscription
        }

        val subscription = TestMqttSubscription(targetClass)
        topicSubscription.add(subscription)
        mqttClient?.subscribe(topic, 1, subscription)

        return subscription
    }

    /**
     * Publishes message to MQTT topic
     *
     * @param topic topic
     * @param message message
     */
    fun <T> publish(topic: String, message: T) {
        mqttClient?.publish(
            "${baseTopic}/test/$topic",
            jacksonObjectMapper().writeValueAsBytes(message),
            0,
            false
        )
    }

    /**
     * Disconnects from MQTT server and sets MQTT client to null
     */
    private fun disconnect() {
        mqttClient?.disconnect()
        mqttClient = null
    }

    override fun close() {
        disconnect()
    }
}