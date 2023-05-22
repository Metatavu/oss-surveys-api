package fi.metatavu.oss.api.metadata

import fi.metatavu.oss.api.impl.translate.AbstractTranslator
import javax.enterprise.context.ApplicationScoped

/**
 * Translates JPA additional metadata info into REST Metadata object
 */
@ApplicationScoped
class MetadataTranslator : AbstractTranslator<DBMetadata, fi.metatavu.oss.api.model.Metadata>() {
    override suspend fun translate(entity: DBMetadata): fi.metatavu.oss.api.model.Metadata {
        return fi.metatavu.oss.api.model.Metadata(
            createdAt = entity.createdAt,
            modifiedAt = entity.modifiedAt,
            creatorId = entity.creatorId,
            lastModifierId = entity.lastModifierId
        )
    }

}
