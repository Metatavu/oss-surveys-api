package fi.metatavu.oss.api.impl.layouts

import fi.metatavu.oss.api.impl.translate.AbstractTranslator
import fi.metatavu.oss.api.model.Layout
import fi.metatavu.oss.api.model.LayoutVariable
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Translator for translating JPA layouts into REST layouts
 */
@ApplicationScoped
class LayoutTranslator: AbstractTranslator<LayoutEntity, Layout>() {

    @Inject
    lateinit var layoutVariableRepository: LayoutVariableRepository

    override suspend fun translate(entity: LayoutEntity): Layout {
        return Layout(
            id = entity.id,
            name = entity.name,
            thumbnail = entity.thumbnailUrl,
            html = entity.html,
            layoutVariables = layoutVariableRepository.listByLayout(entity)
                .map {
                    LayoutVariable(
                        type = it.variabletype,
                        key = it.variablekey
                    )
                },
            metadata = metadataTranslator.translate(entity)
        )
    }

}
