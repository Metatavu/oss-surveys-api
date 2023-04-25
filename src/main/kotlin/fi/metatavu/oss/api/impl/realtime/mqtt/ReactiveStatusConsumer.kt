package fi.metatavu.oss.api.impl.realtime.mqtt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.smallrye.reactive.messaging.mqtt.ReceivingMqttMessageMetadata
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import java.util.concurrent.CompletionStage
import javax.enterprise.context.ApplicationScoped
import kotlin.jvm.optionals.getOrNull

data class DeviceStatus(
    val online: Boolean
)

/**
 * Reactive MQTT Consumer for status messages
 */
@ApplicationScoped
class ReactiveStatusConsumer: AbstractReactiveConsumer<DeviceStatus>() {

    override val channelName: String = "status"

    @OptIn(ExperimentalStdlibApi::class)
    @Incoming(CHANNEL_NAME)
    fun consumeSurveyStatus(message: Message<ByteArray>): CompletionStage<Void> {
        val publisherDeviceSerialNumber = message.metadata
            .get(ReceivingMqttMessageMetadata::class.java)
            .getOrNull()?.topic?.filter { it.isDigit() }
        println("Received device status message from device $publisherDeviceSerialNumber")
        println("Message payload: ${String(message.payload)}")
        val deviceStatus = jacksonObjectMapper().readValue<DeviceStatus>(String(message.payload))
        println("Device online: ${deviceStatus.online}")
        return message.ack()
    }

    companion object {
        private const val CHANNEL_NAME = "status"
    }
}