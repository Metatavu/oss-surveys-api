package fi.metatavu.oss.api.impl.requests

import fi.metatavu.oss.api.model.DeviceRequest
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Controller class for devices
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
    suspend fun createDeviceRequest(serialNumber: String): DeviceRequestEntity {
        return deviceRequestRepository.create(serialNumber = serialNumber)
    }

    /**
     * Finds a Device Request by ID
     *
     * @param id id
     * @return uni with found device request
     */
    suspend fun findDeviceRequest(id: UUID): DeviceRequestEntity? {
        return deviceRequestRepository.findById(id).awaitSuspending()
    }

    /**
     * Finds a Device Request by serial number
     *
     * @param serialNumber serial number
     * @return uni with found device request
     */
    suspend fun findDeviceRequest(serialNumber: String): DeviceRequestEntity? {
        return deviceRequestRepository.findBySerialNumber(serialNumber = serialNumber)
    }

    /**
     * Deletes a Device Request
     *
     * @param deviceRequest device request
     */
    suspend fun deleteDeviceRequest(deviceRequest: DeviceRequestEntity) {
        deviceRequestRepository.deleteSuspending(deviceRequest)
    }

    /**
     * Updates a Device Request
     *
     * @param foundDeviceRequest found device request
     * @param updatedDeviceRequest updated device request
     * @param userId user id
     * @return uni with updated device request
     */
    suspend fun updateDeviceRequest(
        foundDeviceRequest: DeviceRequestEntity,
        updatedDeviceRequest: DeviceRequest,
        userId: UUID
    ): DeviceRequestEntity {
        foundDeviceRequest.id = foundDeviceRequest.id
        foundDeviceRequest.approvalStatus = updatedDeviceRequest.approvalStatus!!
        foundDeviceRequest.lastModifierId = userId

        return deviceRequestRepository.update(deviceRequest = foundDeviceRequest)
    }
}