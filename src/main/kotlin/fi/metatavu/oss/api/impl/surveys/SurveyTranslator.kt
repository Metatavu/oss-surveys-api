package fi.metatavu.oss.api.impl.surveys

import fi.metatavu.oss.api.impl.translate.AbstractTranslator
import javax.enterprise.context.ApplicationScoped

/**
 * Translates DB Survey entity to REST Survey resource
 */
@ApplicationScoped
class SurveyTranslator: AbstractTranslator<SurveyEntity, fi.metatavu.oss.api.model.Survey>() {

    override suspend fun translate(entity: SurveyEntity): fi.metatavu.oss.api.model.Survey {
        return fi.metatavu.oss.api.model.Survey(
            id = entity.id,
            title = entity.title,
            status = entity.status,
            metadata = translateMetadata(entity)
        )
    }

}
