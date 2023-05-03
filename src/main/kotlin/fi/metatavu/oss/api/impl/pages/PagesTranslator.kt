package fi.metatavu.oss.api.impl.pages

import fi.metatavu.oss.api.impl.translate.AbstractTranslator
import fi.metatavu.oss.api.metadata.MetadataTranslator
import fi.metatavu.oss.api.model.Page
import fi.metatavu.oss.api.model.PageProperty
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Translates JPA survey page to REST page object
 */
@ApplicationScoped
class PagesTranslator: AbstractTranslator<PageEntity, Page>() {

    @Inject
    lateinit var pagePropertyRepo: PagePropertyRepository

    @Inject
    lateinit var metadataTranslator: MetadataTranslator

    override suspend fun translate(entity: PageEntity): Page {
        return Page(
            id = entity.id,
            title = entity.title,
            html = entity.html,
            properties = pagePropertyRepo.listByPage(entity).map {
                PageProperty(
                    key = it.propertyKey,
                    value = it.value,
                    type = it.type
                )
            },
            metadata = metadataTranslator.translate(entity)
        )
    }

}
