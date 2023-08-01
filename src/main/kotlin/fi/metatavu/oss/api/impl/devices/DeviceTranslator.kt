package fi.metatavu.oss.api.impl.devices

import fi.metatavu.oss.api.impl.translate.AbstractTranslator
import fi.metatavu.oss.api.model.Device
import fi.metatavu.oss.api.model.DeviceStatus
import java.time.OffsetDateTime
import javax.enterprise.context.ApplicationScoped

/**
 * Translates DB Device entity to REST Device resource
 */
@ApplicationScoped
class DeviceTranslator: AbstractTranslator<DeviceEntity, Device>() {

    override suspend fun translate(entity: DeviceEntity): Device {
        val deviceLastSeen = entity.lastSeen
        val deviceStatus = if (deviceLastSeen.isBefore(OffsetDateTime.now().minusMinutes(5))) {
            DeviceStatus.OFFLINE
        } else {
            entity.deviceStatus
        }

        return Device(
            id = entity.id,
            name = entity.name,
            serialNumber = entity.serialNumber,
            description = entity.description,
            location = entity.location,
            deviceStatus = deviceStatus,
            metadata = translateMetadata(entity)
        )
    }
}