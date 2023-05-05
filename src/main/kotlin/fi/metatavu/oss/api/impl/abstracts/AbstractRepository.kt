package fi.metatavu.oss.api.impl.abstracts

import io.quarkus.hibernate.reactive.panache.PanacheQuery
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase
import io.quarkus.panache.common.Parameters
import io.smallrye.mutiny.coroutines.awaitSuspending

/**
 * Abstract repository with additional methods for listing the entities
 */
abstract class AbstractRepository<Entity, Id> : PanacheRepositoryBase<Entity, Id> {

    /**
     * Lists all entities
     *
     * @param page page
     * @param pageSize page size
     * @return entities
     */
    open suspend fun listAllWithPaging(page: Int? = null, pageSize: Int? = null): Pair<List<Entity>, Long> {
        val count = findAll().count().awaitSuspending()

        return if (page != null && pageSize != null) {
            Pair(findAll().page<Entity>(page, pageSize).list<Entity>().awaitSuspending(), count)
        } else {
            Pair(listAll().awaitSuspending(), count)
        }
    }

    /**
     * Lists with filtering
     *
     * @param queryString query string
     * @param parameters parameters
     * @param page page index
     * @param pageSize page size
     * @return list surveys and count
     */
    suspend fun listWithFilters(
        queryString: String,
        parameters: Parameters,
        page: Int?,
        pageSize: Int?
    ): Pair<List<Entity>, Long> {
        return applyPagingToQuery(
            query = find(queryString, parameters),
            page = page,
            pageSize = pageSize
        )
    }

    /**
     * Applies range to query and executes it
     *
     * @param query find query
     * @param firstIndex first index
     * @param lastIndex last index
     * @return entities
     */
    open suspend fun applyRangeToQuery(
        query: PanacheQuery<Entity>,
        firstIndex: Int?,
        lastIndex: Int?
    ): Pair<List<Entity>, Long> {
        val count = query.count().awaitSuspending()
        return if (firstIndex != null && lastIndex != null) {
            Pair(query.range<Entity>(firstIndex, lastIndex).list<Entity>().awaitSuspending(), count)
        } else
            Pair(query.list<Entity>().awaitSuspending(), count)
    }

    /**
     * Persists suspending
     *
     * @param entity entity
     * @return persisted entity
     */
    open suspend fun persistSuspending(entity: Entity): Entity {
        return persist(entity).awaitSuspending()
    }

    /**
     * Deletes suspending
     *
     * @param entity entity
     */
    open suspend fun deleteSuspending(entity: Entity) {
        delete(entity).awaitSuspending()
    }

    /**
     * Applies paging to query and executes it
     *
     * @param query PanacheQuery<Entity>
     * @param page page index
     * @param pageSize page size
     * @return MutableList<Entity>?
     */
    protected suspend fun applyPagingToQuery(
        query: PanacheQuery<Entity>,
        page: Int?,
        pageSize: Int?
    ): Pair<List<Entity>, Long> {
        val count = query.count().awaitSuspending()
        return if (page != null && pageSize != null) {
            Pair(query.page<Entity>(page, pageSize).list<Entity>().awaitSuspending(), count)
        } else
            Pair(query.list<Entity>().awaitSuspending(), count)
    }
}