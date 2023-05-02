package fi.metatavu.oss.api.impl.devices

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import io.smallrye.mutiny.Uni
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
    fun create(device: DeviceEntity): Uni<DeviceEntity> {
        return persist(device)
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
    fun update(
        device: DeviceEntity,
        name: String,
        description: String?,
        location: String?,
        lastModifierId: UUID,
    ): Uni<DeviceEntity> {
        device.name = name
        device.description = description
        device.location = location
        device.lastModifierId = lastModifierId

        return persist(device)
    }
}