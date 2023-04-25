package fi.metatavu.oss.api.impl.devices

import fi.metatavu.oss.api.impl.requests.DeviceRequestEntity
import fi.metatavu.oss.api.model.DeviceStatus
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.security.PublicKey
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Controller class for devices
 */
@ApplicationScoped
class DeviceController {

    @Inject
    lateinit var deviceRepository: DeviceRepository

    /**
     * Creates a device
     *
     * @param deviceRequest device request
     * @param deviceKey device key
     * @param userId user id
     * @return created device
     */
    suspend fun createDevice(
        deviceRequest: DeviceRequestEntity,
        deviceKey: PublicKey,
        userId: UUID
    ): DeviceEntity {
        val newDevice = DeviceEntity()
        newDevice.id = deviceRequest.id
        newDevice.serialNumber = deviceRequest.serialNumber
        newDevice.deviceKey = deviceKey.encoded
        newDevice.deviceStatus = DeviceStatus.OFFLINE
        newDevice.creatorId = userId
        newDevice.lastModifierId = userId

        return deviceRepository.create(device = newDevice)
    }

    /**
     * Gets device private key by device id
     *
     * @param id id
     * @return device private key base 64 encoded
     */
    suspend fun getDeviceKey(id: UUID): ByteArray {
        return deviceRepository.findById(id).awaitSuspending().deviceKey
    }

    /**
     * Finds a Device
     *
     * @param id id
     * @return found device
     */
    suspend fun findDevice(id: UUID): DeviceEntity? {
        return deviceRepository.findById(id).awaitSuspending()
    }

    /**
     * Finds a Device by serial number
     *
     * @param serialNumber serial number
     * @return found device
     */
    suspend fun findDevice(serialNumber: String): DeviceEntity? {
        return deviceRepository.findBySerialNumber(serialNumber)
    }

    /**
     * Deletes a Device
     *
     * @param device device
     */
    suspend fun deleteDevice(device: DeviceEntity) {
        deviceRepository.deleteSuspending(device)
    }

    /**
     * Lists devices
     *
     * @param firstResult first result
     * @param maxResults max results
     * @return list of devices and count
     */
    suspend fun listDevices(firstResult: Int?, maxResults: Int?): Pair<List<DeviceEntity>, Long> {
        return deviceRepository.listAllWithPaging(
            page = firstResult,
            pageSize = maxResults
        )
    }

    /**
     * Updates Devices status
     *
     * @param device device to update
     * @param status status
     */
    suspend fun updateDeviceStatus(device: DeviceEntity, status: DeviceStatus) {
        deviceRepository.updateDeviceStatus(device, status)
    }
}