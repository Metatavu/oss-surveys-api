package fi.metatavu.oss.api.impl.devices.requests

import fi.metatavu.oss.api.model.DeviceRequest
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional
import io.smallrye.mutiny.Uni
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Controller class for devices (transactions should start here)
 */
@ApplicationScoped
class DeviceRequestController {

    @Inject
    lateinit var deviceRequestRepository: DeviceRequestRepository

    /**
     * Creates a Device Request
     *
     * @param serialNumber serial number
     * @return uni with created device request
     */
    @ReactiveTransactional
    fun createDeviceRequest(serialNumber: String): Uni<DeviceRequestEntity> {
        return deviceRequestRepository.create(serialNumber = serialNumber)
    }

    /**
     * Finds a Device Request by ID
     *
     * @param id id
     * @return uni with found device request
     */
    fun findDeviceRequest(id: UUID): Uni<DeviceRequestEntity?> {
        return deviceRequestRepository.findById(id)
    }

    /**
     * Finds a Device Request by serial number
     *
     * @param serialNumber serial number
     * @return uni with found device request
     */
    fun findDeviceRequest(serialNumber: String): Uni<DeviceRequestEntity?> {
        return deviceRequestRepository.findBySerialNumber(serialNumber = serialNumber)
    }

    /**
     * Deletes a Device Request
     *
     * @param deviceRequest device request
     */
    @ReactiveTransactional
    fun deleteDeviceRequest(deviceRequest: DeviceRequestEntity): Uni<Void> {
        return deviceRequestRepository.delete(deviceRequest)
    }

    /**
     * Updates a Device Request
     *
     * @param foundDeviceRequest found device request
     * @param updatedDeviceRequest updated device request
     * @param userId user id
     * @return uni with updated device request
     */
    @ReactiveTransactional
    fun updateDeviceRequest(
        foundDeviceRequest: DeviceRequestEntity,
        updatedDeviceRequest: DeviceRequest,
        userId: UUID
    ): Uni<DeviceRequestEntity> {
        foundDeviceRequest.id = foundDeviceRequest.id
        foundDeviceRequest.approvalStatus = updatedDeviceRequest.approvalStatus!!
        foundDeviceRequest.lastModifierId = userId

        return deviceRequestRepository.update(deviceRequest = foundDeviceRequest)
    }
}