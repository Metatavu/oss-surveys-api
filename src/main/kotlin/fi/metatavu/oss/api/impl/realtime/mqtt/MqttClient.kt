package fi.metatavu.oss.api.impl.realtime.mqtt

import fi.metatavu.oss.api.impl.devices.DeviceController
import fi.metatavu.oss.api.model.DeviceStatus
import io.netty.handler.codec.mqtt.MqttQoS
import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.mqtt.MqttClient
import io.vertx.mqtt.MqttClientOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.Logger
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.event.Observes
import javax.inject.Inject

/**
 * MQTT client
 */
@ApplicationScoped
class MqttClient {

    @Inject
    lateinit var vertx: Vertx

    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var deviceController: DeviceController

    @Inject
    @ConfigProperty(name = "mqtt.server.url")
    private lateinit var mqttServerUrl: String

    @Inject
    @ConfigProperty(name = "mqtt.base.topic")
    private lateinit var mqttBaseTopic: String

    @Inject
    @ConfigProperty(name = "environment")
    private lateinit var environment: String

    private lateinit var client: MqttClient

    /**
     * Initializes MQTT Client on CDI container startup
     *
     * Subscribes and setups handler device status topic
     */
    @Suppress("unused")
    fun onStartup(@Observes event: StartupEvent) {
        val statusTopic = "$mqttBaseTopic/$environment/+/status"
        val mqttClientOptions = MqttClientOptions()
        mqttClientOptions.clientId = UUID.randomUUID().toString()
        val mqttServerHost = mqttServerUrl.substringBeforeLast(":")
        val mqttServerPort = mqttServerUrl.substringAfterLast(":")
        client = MqttClient.create(vertx, mqttClientOptions)
        client.connect(mqttServerPort.toInt(), mqttServerHost) {
            if (it.succeeded()) {
                logger.info("Connected to MQTT server")
                client.subscribe(statusTopic, MqttQoS.AT_LEAST_ONCE.value()) { result ->
                    if (result.succeeded()) {
                        logger.info("Subscribed to MQTT topic $statusTopic")
                    } else {
                        logger.error("Failed to subscribe to MQTT topic $statusTopic", result.cause())
                    }
                }
            } else {
                logger.error("Failed to connect to MQTT server", it.cause())
            }
        }
        client.publishHandler { message ->
            val senderSerialNumber = message.topicName().filter { it.isDigit() }
            CoroutineScope(vertx.dispatcher()).async {
                val device = deviceController.findDevice(senderSerialNumber)
                if (device == null) {
                    logger.error("Failed to find device with serial number $senderSerialNumber")
                    return@async
                }
                deviceController.updateDeviceStatus(device, DeviceStatus.ONLINE)

                return@async
            }
            logger.info("Received MQTT message on topic ${message.topicName()}")
        }

    }

    /**
     * Disconnects the MQTT client on CDI container shutdown event
     */
    @Suppress("unused")
    fun onShutdown(@Observes event: ShutdownEvent) {
        client.disconnect {
            if (it.succeeded()) {
                logger.info("Disconnected from MQTT server")
            } else {
                logger.error("Failed to disconnect from MQTT server", it.cause())
            }
        }
    }

    /**
     * Publishes message into given MQTT topic
     *
     *
     * @param topic MQTT topic
     * @param payload message to publish
     * @param qos MQTT QoS
     */
    fun publish(topic: String, payload: String, qos: MqttQoS = MqttQoS.AT_LEAST_ONCE) {
        if (client.isConnected) {
            client.publish(topic, Buffer.buffer(payload), qos, false, false)
        } else {
            logger.error("Failed to publish MQTT message, client is not connected")
        }
    }
}