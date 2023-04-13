package fi.metatavu.oss.api.impl.devices

import fi.metatavu.oss.api.impl.requests.DeviceRequestEntity
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.security.PublicKey
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Controller class for devices (transactions should start here)
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
     * @return uni with created device
     */
    @ReactiveTransactional
    fun createDevice(
        deviceRequest: DeviceRequestEntity,
        deviceKey: PublicKey,
        userId: UUID
    ): Uni<DeviceEntity> {
        val newDevice = DeviceEntity()
        newDevice.id = deviceRequest.id
        newDevice.serialNumber = deviceRequest.serialNumber
        newDevice.deviceKey = deviceKey.encoded
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
     * @return uni with found device
     */
    fun findDevice(id: UUID): Uni<DeviceEntity?> {
        return deviceRepository.findById(id)
    }

    /**
     * Deletes a Device
     *
     * @param device device
     */
    @ReactiveTransactional
    fun deleteDevice(device: DeviceEntity): Uni<Void> {
        return deviceRepository.delete(device)
    }
}