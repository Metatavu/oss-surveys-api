package fi.metatavu.oss.api.impl.requests

import fi.metatavu.oss.api.impl.AbstractApi
import fi.metatavu.oss.api.impl.UserRole
import fi.metatavu.oss.api.impl.crypto.CryptoController
import fi.metatavu.oss.api.impl.devices.DeviceController
import fi.metatavu.oss.api.model.DeviceApprovalStatus
import fi.metatavu.oss.api.model.DeviceKey
import fi.metatavu.oss.api.model.DeviceRequest
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import java.util.*
import javax.annotation.security.RolesAllowed
import javax.enterprise.context.RequestScoped
import javax.inject.Inject
import javax.ws.rs.core.Response

@RequestScoped
@Suppress ("unused")
@OptIn(ExperimentalCoroutinesApi::class)
class DeviceRequestsApiImpl: fi.metatavu.oss.api.spec.DeviceRequestsApi, AbstractApi() {

    @Inject
    lateinit var deviceController: DeviceController

    @Inject
    lateinit var deviceRequestController: DeviceRequestController

    @Inject
    lateinit var cryptoController: CryptoController

    @Inject
    lateinit var deviceRequestTranslator: DeviceRequestTranslator

    @Inject
    lateinit var vertx: Vertx
    @ReactiveTransactional
    override fun createDeviceRequest(serialNumber: String): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val existingDeviceRequest = deviceRequestController.findDeviceRequest(serialNumber = serialNumber)

        if (existingDeviceRequest != null) {
            return@async createBadRequest("Duplicate serial number")
        }

        val createdDeviceRequest = deviceRequestController
            .createDeviceRequest(serialNumber = serialNumber)

        createCreated(deviceRequestTranslator.translate(createdDeviceRequest))
    }.asUni()

    @ReactiveTransactional
    @RolesAllowed(UserRole.MANAGER.name)
    override fun deleteDeviceRequest(requestId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val foundDeviceRequest = deviceRequestController
            .findDeviceRequest(id = requestId) ?: return@async createNotFound(DEVICE_REQUEST)

        deviceRequestController.deleteDeviceRequest(deviceRequest = foundDeviceRequest)

        createNoContent()
    }.asUni()

    @ReactiveTransactional
    override fun getDeviceKey(requestId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val foundDeviceRequest = deviceRequestController
            .findDeviceRequest(id = requestId) ?: return@async createNotFound(DEVICE_REQUEST)

        if (foundDeviceRequest.approvalStatus == DeviceApprovalStatus.APPROVED) {
            val keypair = cryptoController.generateRsaKeyPair()
                ?: return@async createInternalServerError("Couldn't create keypair")

            deviceController.createDevice(
                deviceRequest = foundDeviceRequest,
                deviceKey = keypair.public,
                userId = foundDeviceRequest.lastModifierId!!
            )

            deviceRequestController.deleteDeviceRequest(
                deviceRequest = foundDeviceRequest
            )

            return@async createOk(DeviceKey(
                    key = cryptoController.getPrivateKeyBase64(keypair.private)
            ))
        }

        createForbidden(FORBIDDEN)
    }.asUni()

    @RolesAllowed(UserRole.MANAGER.name)
    override fun listDeviceRequests(firstResult: Int?, maxResults: Int?): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val (start, end) = firstMaxToRange(firstResult, maxResults)

        val (deviceRequests, count) = deviceRequestController.listDeviceRequests(
            rangeStart = start,
            rangeEnd = end
        )

        createOk(deviceRequestTranslator.translate(deviceRequests), count)
    }.asUni()

    @ReactiveTransactional
    @RolesAllowed(UserRole.MANAGER.name)
    override fun updateDeviceRequest(requestId: UUID, deviceRequest: DeviceRequest): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        val foundDeviceRequest = deviceRequestController.findDeviceRequest(id = requestId)
            ?: return@async  createNotFound(DEVICE_REQUEST)

        val updatedDeviceRequest = deviceRequestController.updateDeviceRequest(
            foundDeviceRequest = foundDeviceRequest,
            updatedDeviceRequest =  deviceRequest,
            userId = userId
        )

        createOk(deviceRequestTranslator.translate(updatedDeviceRequest))
    }.asUni()
}