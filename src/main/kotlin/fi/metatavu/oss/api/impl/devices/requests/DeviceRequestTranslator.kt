package fi.metatavu.oss.api.impl.devices.requests

import fi.metatavu.oss.api.impl.translate.AbstractTranslator
import fi.metatavu.oss.api.model.DeviceRequest
import fi.metatavu.oss.api.model.Metadata
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class DeviceRequestTranslator: AbstractTranslator<DeviceRequestEntity, DeviceRequest>() {

    override fun translate(entity: DeviceRequestEntity): DeviceRequest {
        return DeviceRequest(
            id = entity.id,
            serialNumber = entity.serialNumber,
            approvalStatus = entity.approvalStatus,
            metadata = Metadata(
                createdAt = entity.createdAt,
                modifiedAt = entity.modifiedAt,
                creatorId = entity.creatorId,
                lastModifierId = entity.lastModifierId
            )
        )
    }
}