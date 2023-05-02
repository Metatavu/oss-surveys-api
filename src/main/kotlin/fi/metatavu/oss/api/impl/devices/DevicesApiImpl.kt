package fi.metatavu.oss.api.impl.devices

import fi.metatavu.oss.api.impl.AbstractApi
import fi.metatavu.oss.api.impl.UserRole
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import java.util.*
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response

@RequestScoped
@Suppress ("unused")
@OptIn(ExperimentalCoroutinesApi::class)
class DevicesApiImpl: fi.metatavu.oss.api.spec.DevicesApi, AbstractApi() {

    @Inject
    lateinit var deviceController: DeviceController

    @Inject
    lateinit var vertx: Vertx

    @ReactiveTransactional
    @RolesAllowed(UserRole.MANAGER.name)
    override fun deleteDevice(deviceId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val foundDevice = deviceController.findDevice(id = deviceId) ?: return@async createNotFound(DEVICE)

        deviceController.deleteDevice(device = foundDevice)

        createNoContent()
    }.asUni()
}