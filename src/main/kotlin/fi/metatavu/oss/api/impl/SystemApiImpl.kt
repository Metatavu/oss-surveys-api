package fi.metatavu.oss.api.impl

import fi.metatavu.oss.api.impl.realtime.mqtt.DeviceStatus
import fi.metatavu.oss.api.impl.realtime.mqtt.MqttClient
import fi.metatavu.oss.api.impl.realtime.mqtt.ReactiveStatusProducer
import fi.metatavu.oss.api.spec.SystemApi
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import javax.enterprise.context.RequestScoped
import javax.inject.Inject
import javax.ws.rs.core.Response

/**
 * System API implementation
 *
 * @author Antti Leppä
 */
@RequestScoped
@Suppress ("unused")
@OptIn(ExperimentalCoroutinesApi::class)
class SystemApiImpl: SystemApi, AbstractApi()  {

    @Inject
    lateinit var vertx: Vertx

    @Inject
    lateinit var mqttClient: MqttClient

    @Inject
    lateinit var reactiveStatusProducer: ReactiveStatusProducer

    override fun ping(): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        // mqttClient.publish("oss/staging/1234/surveys/create", "{\"status\":false}")
        reactiveStatusProducer.sendStatusMessage(DeviceStatus(false))
        createOk("pong")
    }.asUni()

}
