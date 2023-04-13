package fi.metatavu.oss.api.impl.devices

import fi.metatavu.oss.api.impl.AbstractApi
import fi.metatavu.oss.api.impl.UserRole
import fi.metatavu.oss.api.impl.crypto.CryptoController
import fi.metatavu.oss.api.impl.devices.requests.DeviceRequestController
import fi.metatavu.oss.api.impl.devices.requests.DeviceRequestTranslator
import fi.metatavu.oss.api.model.DeviceApprovalStatus
import fi.metatavu.oss.api.model.DeviceRequest
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

    @Inject
    lateinit var deviceRequestController: DeviceRequestController

    @Inject
    lateinit var cryptoController: CryptoController

    @Inject
    lateinit var deviceRequestTranslator: DeviceRequestTranslator

    override suspend fun createDeviceRequest(serialNumber: String): Response {
        val existingDevice = deviceRequestController.findDeviceRequest(serialNumber = serialNumber).awaitSuspending()

        if (existingDevice != null) {
            return createBadRequest("Duplicate serial number")
        }

        val createdDeviceRequest = deviceRequestController
            .createDeviceRequest(serialNumber = serialNumber)
            .awaitSuspending()

        return createCreated(deviceRequestTranslator.translate(createdDeviceRequest))
    }

    override suspend fun getDeviceKey(requestId: UUID): Response {
        val foundDeviceRequest = deviceRequestController
            .findDeviceRequest(id = requestId)
            .awaitSuspending() ?: return createNotFound(DEVICEREQUEST)

        if (foundDeviceRequest.approvalStatus == DeviceApprovalStatus.APPROVED) {
            val keypair = cryptoController.generateRsaKeyPair()
                ?: return createInternalServerError("Couldn't create keypair")

            deviceController.createDevice(
                deviceRequest = foundDeviceRequest,
                deviceKey = keypair.public,
                userId = foundDeviceRequest.lastModifierId!!
            ).awaitSuspending()

            deviceRequestController.deleteDeviceRequest(
                deviceRequest = foundDeviceRequest
            ).awaitSuspending()

            return createOk(cryptoController.getPrivateKeyBase64(keypair.private))
        }

        return createForbidden(FORBIDDEN)
    }

    @RolesAllowed(UserRole.MANAGER.name)
    override suspend fun updateDeviceRequest(requestId: UUID, deviceRequest: DeviceRequest): Response {
        val userId = loggedUserId ?: return createUnauthorized(UNAUTHORIZED)
        val foundDeviceRequest = deviceRequestController.findDeviceRequest(id = requestId).awaitSuspending()
            ?: return createNotFound(DEVICEREQUEST)

        val updatedDeviceRequest = deviceRequestController.updateDeviceRequest(
            foundDeviceRequest = foundDeviceRequest,
            updatedDeviceRequest =  deviceRequest,
            userId = userId
        ).awaitSuspending()

        return createOk(deviceRequestTranslator.translate(updatedDeviceRequest))
    }
}