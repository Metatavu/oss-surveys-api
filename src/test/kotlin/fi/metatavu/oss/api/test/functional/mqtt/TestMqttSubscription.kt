package fi.metatavu.oss.api.test.functional.mqtt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okio.Buffer
import org.awaitility.Awaitility.await
import org.eclipse.paho.client.mqttv3.IMqttMessageListener
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.util.concurrent.TimeUnit

/**
 * MQTT Subscription for functional tests
 *
 * @param targetClass message class
 */
class TestMqttSubscription <T>(
    private val targetClass: Class<T>
): IMqttMessageListener {

    private val messages: MutableList<T> = mutableListOf()

    /**
     * Returns buffered messages
     *
     * @param waitCount number of messages to wait
     * @return messages
     */
    fun getMessages(waitCount: Int): List<T> {
        await().atMost(1, TimeUnit.MINUTES).until { messages.size >= waitCount }

        return messages.toList()
    }

    /**
     * Callback for receiving messages in given topic.
     *
     * Writes received messages to buffer.
     *
     * @param topic topic
     * @param message message
     */
    override fun messageArrived(topic: String, message: MqttMessage) {
        val buffer = Buffer()
        buffer.write(message.payload)
        val parsedMessage = jacksonObjectMapper().readValue(buffer.readByteArray(), targetClass)
        if (parsedMessage != null) {
            if (!messages.contains(parsedMessage)) {
                messages.add(parsedMessage)
            }
        }
    }
}