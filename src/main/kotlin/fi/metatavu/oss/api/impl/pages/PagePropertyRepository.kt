package fi.metatavu.oss.api.impl.pages

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import fi.metatavu.oss.api.model.PagePropertyType
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import java.util.*

/**
 * A repository class for page properties
 */
@ApplicationScoped
class PagePropertyRepository: AbstractRepository<PagePropertyEntity, UUID>() {

    /**
     * Lists page properties by page
     *
     * @param entity page
     * @return list of page properties
     */
    suspend fun listByPage(entity: PageEntity): List<PagePropertyEntity> {
        return list("page = ?1", entity).awaitSuspending()
    }

    /**
     * Creates a new page property
     *
     * @param id id
     * @param key key
     * @param value value
     * @param type type
     * @param page page
     * @return created page property
     */
    suspend fun create(id: UUID, key: String, value: String, type: PagePropertyType, page: PageEntity): PagePropertyEntity {
        val pagePropertyEntity = PagePropertyEntity()
        pagePropertyEntity.id = id
        pagePropertyEntity.propertyKey = key
        pagePropertyEntity.value = value
        pagePropertyEntity.type = type
        pagePropertyEntity.page = page
        return persist(pagePropertyEntity).awaitSuspending()
    }

}
