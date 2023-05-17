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
        return PageQuestion(
            type = entity.type,
            options = pageQuestionOptionRepository.listByQuestion(entity).map {
                PageQuestionOption(
                    questionOptionValue = it.value,
                    orderNumber = it.orderNumber!!
                )
            }
        )
    }
}