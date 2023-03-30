package fi.metatavu.oss.api.impl.abstracts

import io.quarkus.hibernate.reactive.panache.PanacheQuery
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase
import io.smallrye.mutiny.Uni

abstract class AbstractRepository<Entity, Id> : PanacheRepositoryBase<Entity, Id> {

    /**
     * Lists all entities
     *
     * @param page page
     * @param pageSize page size
     * @return entities
     */
    open fun listAllWithPaging(page: Int? = null, pageSize: Int? = null): Pair<Uni<List<Entity>>, Uni<Long>> {
        val count = findAll().count()
        return if (page != null && pageSize != null) {
            Pair(findAll().page<Entity>(page, pageSize).list(), count)
        } else
            Pair(listAll(), count)
    }

    /**
     * Applies paging to query and executes it
     *
     * @param query PanacheQuery<Entity>
     * @param page page index
     * @param pageSize page size
     * @return MutableList<Entity>?
     */
    open fun applyPagingToQuery(
        query: PanacheQuery<Entity>,
        page: Int?,
        pageSize: Int?
    ): Pair<Uni<List<Entity>>, Uni<Long>> {
        val count = query.count()
        return if (page != null && pageSize != null) {
            Pair(query.page<Entity>(page, pageSize).list(), count)
        } else
            Pair(query.list(), count)
    }

    /**
     * Applies range to query and executes it
     *
     * @param query find query
     * @param firstIndex first index
     * @param lastIndex last index
     * @return entities
     */
    open fun applyRangeToQuery(
        query: PanacheQuery<Entity>,
        firstIndex: Int?,
        lastIndex: Int?
    ): Pair<Uni<List<Entity>>, Uni<Long>> {
        val count = query.count()
        return if (firstIndex != null && lastIndex != null) {
            Pair(query.range<Entity>(firstIndex, lastIndex).list(), count)
        } else
            Pair(query.list(), count)
    }
}