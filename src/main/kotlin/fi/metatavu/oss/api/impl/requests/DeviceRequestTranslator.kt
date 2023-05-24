package fi.metatavu.oss.api.impl.requests

import fi.metatavu.oss.api.impl.translate.AbstractTranslator
import fi.metatavu.oss.api.model.DeviceRequest
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class DeviceRequestTranslator: AbstractTranslator<DeviceRequestEntity, DeviceRequest>() {

    override suspend fun translate(entity: DeviceRequestEntity): DeviceRequest {
        return DeviceRequest(
            id = entity.id,
            serialNumber = entity.serialNumber,
            approvalStatus = entity.approvalStatus,
            name = entity.name,
            description = entity.description,
            location = entity.location,
            metadata = translateMetadata(entity)
        )
    }
}