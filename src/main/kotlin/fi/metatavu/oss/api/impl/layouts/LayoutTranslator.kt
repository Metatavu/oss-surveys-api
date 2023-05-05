package fi.metatavu.oss.api.impl.layouts

import fi.metatavu.oss.api.impl.translate.AbstractTranslator
import fi.metatavu.oss.api.model.Layout
import javax.enterprise.context.ApplicationScoped

/**
 * Translator for translating JPA layouts into REST layouts
 */
@ApplicationScoped
class LayoutTranslator: AbstractTranslator<LayoutEntity, Layout>() {

    override suspend fun translate(entity: LayoutEntity): Layout {
        return Layout(
            id = entity.id,
            name = entity.name,
            thumbnail = entity.thumbnailUrl,
            html = entity.html,
            metadata = metadataTranslator.translate(entity)
        )
    }

}
