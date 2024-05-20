package fi.metatavu.oss.api.impl.pages.answers

import com.fasterxml.jackson.databind.ObjectMapper
import fi.metatavu.oss.api.impl.pages.answers.entities.PageAnswerBaseEntity
import fi.metatavu.oss.api.impl.pages.answers.entities.PageAnswerMulti
import fi.metatavu.oss.api.impl.pages.answers.entities.PageAnswerSingle
import fi.metatavu.oss.api.impl.pages.answers.entities.PageAnswerText
import fi.metatavu.oss.api.impl.pages.answers.repositories.MultiAnswersToOptionsRepository
import fi.metatavu.oss.api.impl.translate.AbstractTranslator
import fi.metatavu.oss.api.model.DevicePageSurveyAnswer
import fi.metatavu.oss.api.model.Metadata
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Translator for page answers
 */
@ApplicationScoped
class PageAnswerTranslator : AbstractTranslator<PageAnswerBaseEntity, DevicePageSurveyAnswer>() {

    @Inject
    lateinit var pageanswerMultiAnswersToOptionsRepository: MultiAnswersToOptionsRepository

    @Inject
    lateinit var objectMapper: ObjectMapper

    override suspend fun translate(entity: PageAnswerBaseEntity): DevicePageSurveyAnswer {
        val answerString = when (entity) {
            is PageAnswerMulti -> objectMapper.writeValueAsString(
                pageanswerMultiAnswersToOptionsRepository.listByPageAnswer(
                    entity
                ).map {
                    it.questionOption.id
                }
            )

            is PageAnswerSingle -> entity.option.id.toString()
            is PageAnswerText -> entity.text
            else -> throw IllegalArgumentException("Unknown PageAnswerBaseEntity type ${entity::class.java.name}")
        }

        return DevicePageSurveyAnswer(
            id = entity.id,
            answer = answerString,
            pageId = entity.page.id,
            metadata = Metadata(
                createdAt = entity.createdAt,
                modifiedAt = entity.modifiedAt,
                creatorId = entity.creatorId,
                lastModifierId = entity.lastModifierId
            )
        )
    }
}