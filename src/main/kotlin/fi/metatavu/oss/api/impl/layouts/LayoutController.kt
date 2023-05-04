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
        return layoutRepository.create(
            id = UUID.randomUUID(),
            name = layout.name,
            thumbnailUrl = layout.thumbnail,
            html = layout.html,
            creatorId = userId
        )
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
     * @param found found layout
     * @param layout layout data
     * @param userId last modifier id
     * @return updated layout
     */
    suspend fun update(found: LayoutEntity, layout: Layout, userId: UUID): LayoutEntity {
        return layoutRepository.update(
            layout = found,
            name = layout.name,
            thumbnailUrl = layout.thumbnail,
            html = layout.html,
            lastModifierId = userId
        )
    }

    /**
     * Deletes a layout
     *
     * @param found found layout
     */
    suspend fun delete(found: LayoutEntity) {
        layoutRepository.deleteSuspending(found)
    }


}
