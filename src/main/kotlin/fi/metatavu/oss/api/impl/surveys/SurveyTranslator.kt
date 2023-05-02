package fi.metatavu.oss.api.impl.surveys

import fi.metatavu.oss.api.impl.translate.AbstractTranslator
import fi.metatavu.oss.api.metadata.MetadataTranslator
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Translates DB Survey entity to REST Survey resource
 */
@ApplicationScoped
class SurveyTranslator: AbstractTranslator<SurveyEntity, fi.metatavu.oss.api.model.Survey>() {

    @Inject
    lateinit var metadataTranslator: MetadataTranslator

    override fun translate(entity: SurveyEntity): fi.metatavu.oss.api.model.Survey {
        return fi.metatavu.oss.api.model.Survey(
            id = entity.id,
            title = entity.title,
            status = entity.status,
            metadata = metadataTranslator.translate(entity)
        )
    }

}
