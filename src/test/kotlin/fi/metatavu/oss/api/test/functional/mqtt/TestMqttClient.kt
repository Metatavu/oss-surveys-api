/* package fi.metatavu.oss.api.test.functional.mqtt

import org.eclipse.microprofile.config.ConfigProvider
import java.util.*

/**
 * MQTT Client for functional tests
 */
class TestMqttClient: AutoCloseable {

    private val subscriptions = mutableMapOf<String, MutableList<TestMqttSubscription<*>>>()

    /**
     * Constructor. Connects to MQTT server
     */
    init {
        MqttConnection.connect(MqttSettings(
            publisherId = UUID.randomUUID().toString(),
            serverUrl = ConfigProvider.getConfig().getValue("mqtt.server.url", String::class.java),
            topic = ConfigProvider.getConfig().getValue("mqtt.topic", String::class.java),
            username = null,
            password = null
        ))

        MqttConnection.setSubscriber(this)
    }

    /**
     * Subscribes to MQTT topic
     *
     * @param targetClass message class
     * @param subscriptionTopic topic to subscribe
     */
    fun <T> subscribe(targetClass: Class<T>, subscriptionTopic: String) {
        val baseTopic = ConfigProvider.getConfig().getValue("mqtt.topic", String::class.java)
        val topic = "$baseTopic/$subscriptionTopic"

    }

    override fun close() {
        TODO("Not yet implemented")
    }
} */