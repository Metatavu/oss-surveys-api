package fi.metatavu.oss.api.impl.devices

import fi.metatavu.oss.api.impl.AbstractApi
import fi.metatavu.oss.api.impl.UserRole
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.util.*
import javax.annotation.security.RolesAllowed
import javax.enterprise.context.RequestScoped
import javax.inject.Inject
import javax.ws.rs.core.Response

@RequestScoped
class DevicesApiImpl: fi.metatavu.oss.api.spec.DevicesApi, AbstractApi() {

    @Inject
    lateinit var deviceController: DeviceController


    @RolesAllowed(UserRole.MANAGER.name)
    override suspend fun deleteDevice(deviceId: UUID): Response {
        val foundDevice = deviceController.findDevice(id = deviceId).awaitSuspending() ?: return createNotFound(DEVICE)

        deviceController.deleteDevice(device = foundDevice).awaitSuspending()

        return createNoContent()
    }
}