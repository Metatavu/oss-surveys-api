package fi.metatavu.oss.api.impl.layouts

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import java.util.*
import javax.enterprise.context.ApplicationScoped

/**
 * Layout repository
 */
@ApplicationScoped
class LayoutRepository: AbstractRepository<LayoutEntity, UUID>() {

    /**
     * Creates new layout
     *
     * @param id id
     * @param name name
     * @param thumbnailUrl thumbnail url
     * @param html html
     * @param creatorId creator id
     * @return created layout
     */
    suspend fun create(
        id: UUID,
        name: String,
        thumbnailUrl: String,
        html: String,
        creatorId: UUID
    ): LayoutEntity {
        val layoutEntity = LayoutEntity()
        layoutEntity.id = id
        layoutEntity.name = name
        layoutEntity.thumbnailUrl = thumbnailUrl
        layoutEntity.html = html
        layoutEntity.creatorId = creatorId
        layoutEntity.lastModifierId = creatorId

        return persistSuspending(layoutEntity)
    }

    /**
     * Updates layout
     *
     * @param layout layout to update
     * @param name name
     * @param thumbnailUrl thumbnail url
     * @param html html
     * @param lastModifierId last modifier id
     * @return updated layout
     */
    suspend fun update(
        layout: LayoutEntity,
        name: String,
        thumbnailUrl: String,
        html: String,
        lastModifierId: UUID
    ): LayoutEntity {
        layout.name = name
        layout.thumbnailUrl = thumbnailUrl
        layout.html = html
        layout.lastModifierId = lastModifierId

        return persistSuspending(layout)
    }

}