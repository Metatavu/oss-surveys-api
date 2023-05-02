package fi.metatavu.oss.api.impl.devices

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import java.util.UUID
import jakarta.enterprise.context.ApplicationScoped

/**
 * Repository class for devices
 */
@ApplicationScoped
class DeviceRepository: AbstractRepository<DeviceEntity, UUID>() {

    /**
     * Creates a device
     *
     * @param device device
     * @return uni with created device
     */
    suspend fun create(device: DeviceEntity): DeviceEntity {
        return persistSuspending(device)
    }

    /**
     * Updates a device
     *
     * @param device device to update
     * @param name name
     * @param description description
     * @param location location
     * @param lastModifierId last modifier id
     * @return uni with updated device
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
}