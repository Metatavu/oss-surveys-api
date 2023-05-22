package fi.metatavu.oss.api.impl.layouts

import fi.metatavu.oss.api.model.Layout
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Controller for page layouts
 */
@ApplicationScoped
class LayoutController {

    @Inject
    lateinit var layoutRepository: LayoutRepository

    @Inject
    lateinit var layoutVariableRepository: LayoutVariableRepository

    /**
     * Lists layouts
     *
     * @param first first index
     * @param last last index
     * @return list of layouts and count
     */
    suspend fun list(first: Int, last: Int): Pair<List<LayoutEntity>, Long> {
        return layoutRepository.applyRangeToQuery(layoutRepository.findAll(), first, last)
    }

    /**
     * Creates a layout
     *
     * @param layout layout data
     * @param userId creator id
     * @return created layout
     */
    suspend fun create(layout: Layout, userId: UUID): LayoutEntity {
        val pageLayout = layoutRepository.create(
            id = UUID.randomUUID(),
            name = layout.name,
            thumbnailUrl = layout.thumbnail,
            html = layout.html,
            creatorId = userId
        )

        layout.layoutVariables?.forEach { restLayoutVar ->
            layoutVariableRepository.create(
                id = UUID.randomUUID(),
                layout = pageLayout,
                type = restLayoutVar.type,
                key = restLayoutVar.key
            )
        }

        return pageLayout
    }

    /**
     * Finds a layout by id
     *
     * @param layoutId layout id
     * @return found layout or null if not found
     */
    suspend fun find(layoutId: UUID): LayoutEntity? {
        return layoutRepository.findById(layoutId).awaitSuspending()
    }

    /**
     * Updates a layout
     *
     * @param layoutEntry found layout to be updated
     * @param layout layout data
     * @param userId last modifier id
     * @return updated layout
     */
    suspend fun update(layoutEntry: LayoutEntity, layout: Layout, userId: UUID): LayoutEntity {
        val updated = layoutRepository.update(
            layout = layoutEntry,
            name = layout.name,
            thumbnailUrl = layout.thumbnail,
            html = layout.html,
            lastModifierId = userId
        )

        //replace variables
        layoutVariableRepository.listByLayout(layoutEntry).forEach {
            layoutVariableRepository.deleteSuspending(it)
        }
        layout.layoutVariables?.forEach { restLayoutVar ->
            layoutVariableRepository.create(
                id = UUID.randomUUID(),
                layout = updated,
                type = restLayoutVar.type,
                key = restLayoutVar.key
            )
        }

        return updated
    }

    /**
     * Deletes a layout
     *
     * @param layoutEntry layout entry to be deleted
     */
    suspend fun delete(layoutEntry: LayoutEntity) {
        layoutVariableRepository.listByLayout(layoutEntry).forEach {
            layoutVariableRepository.deleteSuspending(it)
        }
        layoutRepository.deleteSuspending(layoutEntry)
    }
}
