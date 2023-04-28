package fi.metatavu.oss.api.impl.realtime.mqtt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.metatavu.oss.api.impl.devices.DeviceController
import fi.metatavu.oss.api.model.DeviceStatusMessage
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.slf4j.Logger
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.context.control.ActivateRequestContext
import javax.inject.Inject


/**
 * Reactive MQTT Consumer for status messages
 */
@Suppress("unused")
@ApplicationScoped
class ReactiveStatusConsumer {

    @Inject
    lateinit var deviceController: DeviceController

    @Inject
    private lateinit var logger: Logger

    @Inject
    private lateinit var vertx: Vertx

    @OptIn(ExperimentalCoroutinesApi::class)
    @Suppress("unused")
    @Incoming(CHANNEL_NAME)
    @ActivateRequestContext
    @ReactiveTransactional
    fun consumeSurveyStatus(message: ByteArray): Uni<Void> {
        return CoroutineScope(vertx.dispatcher()).async {
            val deviceStatusMessage = jacksonObjectMapper().readValue<DeviceStatusMessage>(String(message))
            val deviceId = deviceStatusMessage.deviceId
            val status = deviceStatusMessage.status
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
        }.asUni().replaceWithVoid()
    }

    companion object {
        private const val CHANNEL_NAME = "status"
    }
}