package fi.metatavu.oss.api.impl.pages

import fi.metatavu.oss.api.impl.pages.questions.PageQuestionController
import fi.metatavu.oss.api.impl.pages.questions.PageQuestionTranslator
import fi.metatavu.oss.api.impl.translate.AbstractTranslator
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
    lateinit var pageQuestionTranslator: PageQuestionTranslator

    @Inject
    lateinit var pageQuestionController: PageQuestionController

    override suspend fun translate(entity: PageEntity): Page {
        return Page(
            id = entity.id,
            title = entity.title,
            properties = pagePropertyRepo.listByPage(entity).map {
                PageProperty(
                    key = it.propertyKey,
                    value = it.value
                )
            },
            question = pageQuestionController.find(entity)?.let {
                pageQuestionTranslator.translate(it)
            },
            layoutId = entity.layout.id,
            orderNumber = entity.orderNumber,
            metadata = translateMetadata(entity)
        )
    }

}
