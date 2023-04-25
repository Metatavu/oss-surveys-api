package fi.metatavu.oss.api.impl.realtime.mqtt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.smallrye.reactive.messaging.mqtt.ReceivingMqttMessageMetadata
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import java.util.concurrent.CompletionStage
import kotlin.jvm.optionals.getOrNull

/**
 * Abstract base class for reactive MQTT Consumers
 */
abstract class AbstractReactiveConsumer<T> {

        /**
        * Channel name
        */
        abstract open val channelName: String

        /**
        * Consumes message
        *
        * @param message message
        */
        @OptIn(ExperimentalStdlibApi::class)
        protected inline fun <reified T> consumeMessage(message: Message<ByteArray>): CompletionStage<Void> {
            val publisherDeviceSerialNumber = message.metadata
                .get(ReceivingMqttMessageMetadata::class.java)
                .getOrNull()?.topic?.filter { it.isDigit() }
            jacksonObjectMapper().readValue<T>(message.payload.toString())

            return message.ack()
        }
}