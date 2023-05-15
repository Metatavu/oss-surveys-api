package fi.metatavu.oss.api.impl.layouts

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import fi.metatavu.oss.api.model.LayoutVariableType
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.util.*
import javax.enterprise.context.ApplicationScoped

/**
 * Repository for layout variables
 */
@ApplicationScoped
class LayoutVariableRepository : AbstractRepository<LayoutVariableEntity, UUID>() {

    /**
     * Creates layout variable for a layout
     *
     * @param id id
     * @param layout layout
     * @param type type
     * @param key key
     * @return created layout variable
     */
    suspend fun create(id: UUID, layout: LayoutEntity, type: LayoutVariableType, key: String): LayoutVariableEntity {
        val entity = LayoutVariableEntity()
        entity.id = id
        entity.layout = layout
        entity.variabletype = type
        entity.variablekey = key
        return persistSuspending(entity)
    }

    /**
     * Lists layout variables by layout
     *
     * @param layout layout
     * @return list of layout variables
     */
    suspend fun listByLayout(layout: LayoutEntity): List<LayoutVariableEntity> {
        return list("layout=?1", layout).awaitSuspending()
    }
}