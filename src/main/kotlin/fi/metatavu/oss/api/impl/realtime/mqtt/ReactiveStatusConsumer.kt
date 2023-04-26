package fi.metatavu.oss.api.impl.realtime.mqtt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.metatavu.oss.api.impl.devices.DeviceController
import fi.metatavu.oss.api.model.DeviceStatusMessage
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import org.slf4j.Logger
import java.util.concurrent.CompletionStage
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject


/**
 * Reactive MQTT Consumer for status messages
 */
@ApplicationScoped
class ReactiveStatusConsumer {

    @Inject
    private lateinit var deviceController: DeviceController

    @Inject
    private lateinit var logger: Logger

    @Inject
    private lateinit var vertx: Vertx

    @Suppress("unused")
    @Incoming(CHANNEL_NAME)
    fun consumeSurveyStatus(message: Message<ByteArray>): CompletionStage<Void> {
        val deviceStatusMessage = jacksonObjectMapper().readValue<DeviceStatusMessage>(String(message.payload))
        val deviceId = deviceStatusMessage.deviceId
        val status = deviceStatusMessage.status

        CoroutineScope(vertx.dispatcher()).launch {
            try {
                val foundDevice = deviceController.findDevice(deviceId)

                if (foundDevice == null) {
                    logger.warn("Received status message from unknown Device $deviceId")
                } else {
                    logger.info("Received status ($status) message from Device $deviceId")
                    deviceController.updateDeviceStatus(
                        device = foundDevice,
                        status = status
                    )
                }
            } catch (e: Exception) {
                logger.error("Failed to process status message from Device $deviceId", e)
            }
        }

        return message.ack()
    }

    companion object {
        private const val CHANNEL_NAME = "status"
    }
}