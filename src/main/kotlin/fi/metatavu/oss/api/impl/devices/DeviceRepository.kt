package fi.metatavu.oss.api.impl.devices

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import fi.metatavu.oss.api.model.DeviceStatus
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.util.UUID
import javax.enterprise.context.ApplicationScoped

/**
 * Repository class for devices
 */
@ApplicationScoped
class DeviceRepository: AbstractRepository<DeviceEntity, UUID>() {

    /**
     * Creates a Device
     *
     * @param device device
     * @return created device
     */
    suspend fun create(device: DeviceEntity): DeviceEntity {
        return persistSuspending(device)
    }

    /**
     * Updates a Device
     *
     * @param device device to update
     * @param name name
     * @param description description
     * @param location location
     * @param lastModifierId last modifier id
     * @return updated device
     */
    suspend fun update(
        device: DeviceEntity,
        name: String,
        description: String?,
        location: String?,
        lastModifierId: UUID,
    ): DeviceEntity {
        device.name = name
        device.description = description
        device.location = location
        device.lastModifierId = lastModifierId

        return persistSuspending(device)
    }

    /**
     * Updates Devices status
     *
     * @param device device to update
     * @param status status
     */
    suspend fun updateDeviceStatus(device: DeviceEntity, status: DeviceStatus) {
        device.deviceStatus = status

        persistSuspending(device)
    }

    /**
     * Finds a Device a by serial number
     *
     * @param serialNumber serial number
     * @return found device
     */
    suspend fun findBySerialNumber(serialNumber: String): DeviceEntity? {
        return find("serialNumber", serialNumber).firstResult<DeviceEntity>().awaitSuspending()
    }
}