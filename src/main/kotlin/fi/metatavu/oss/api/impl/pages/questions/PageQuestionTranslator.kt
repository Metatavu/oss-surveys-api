package fi.metatavu.oss.api.impl.pages.questions

import fi.metatavu.oss.api.impl.translate.AbstractTranslator
import fi.metatavu.oss.api.model.PageQuestion
import fi.metatavu.oss.api.model.PageQuestionOption
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Translates page question JPA to REST objects
 */
@ApplicationScoped
class PageQuestionTranslator : AbstractTranslator<PageQuestionEntity, PageQuestion>() {

    @Inject
    lateinit var pageQuestionOptionRepository: QuestionOptionRepository

    override suspend fun translate(entity: PageQuestionEntity): PageQuestion {
        return translate(entity = entity, supportRichText = true)
    }

    /**
     * Translates page question JPA to REST objects
     *
     * @param entity page question JPA entity
     * @param supportRichText should rich text be supported
     * @return translated page question
     */
    suspend fun translate(entity: PageQuestionEntity, supportRichText : Boolean): PageQuestion {
        return PageQuestion(
            type = entity.type,
            options = pageQuestionOptionRepository.listByQuestion(entity).map {
                PageQuestionOption(
                    questionOptionValue = processHtmlText(html = it.value, supportRichText = supportRichText),
                    orderNumber = it.orderNumber!!,
                    id = it.id
                )
            }
        )
    }

}