package fi.metatavu.oss.api.impl.devicesurveys

import fi.metatavu.oss.api.impl.translate.AbstractTranslator
import fi.metatavu.oss.api.model.DeviceSurvey
import javax.enterprise.context.ApplicationScoped

/**
 * Translates DB Device Survey entity to REST Device Survey resource
 */
@ApplicationScoped
class DeviceSurveyTranslator: AbstractTranslator<DeviceSurveyEntity, DeviceSurvey>() {

    override fun translate(entity: DeviceSurveyEntity): DeviceSurvey {
        return DeviceSurvey(
            id = entity.id,
            surveyId = entity.survey.id,
            deviceId = entity.device.id,
            status = entity.status,
            publishStartTime = entity.publishStartTime,
            publishEndTime = entity.publishEndTime,
            metadata = translateMetadata(entity)
        )
    }
}