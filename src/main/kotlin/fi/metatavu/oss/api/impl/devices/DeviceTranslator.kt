package fi.metatavu.oss.api.impl.devices

import fi.metatavu.oss.api.impl.translate.AbstractTranslator
import fi.metatavu.oss.api.model.Device
import javax.enterprise.context.ApplicationScoped

/**
 * Translates DB Device entity to REST Device resource
 */
@ApplicationScoped
class DeviceTranslator: AbstractTranslator<DeviceEntity, Device>() {

    override suspend fun translate(entity: DeviceEntity): Device {
        return Device(
            id = entity.id,
            name = entity.name,
            serialNumber = entity.serialNumber,
            description = entity.description,
            location = entity.location,
            deviceStatus = entity.deviceStatus,
            metadata = translateMetadata(entity)
        )
    }
}