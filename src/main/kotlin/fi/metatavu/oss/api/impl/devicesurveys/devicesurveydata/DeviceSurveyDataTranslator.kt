package fi.metatavu.oss.api.impl.devicesurveys.devicesurveydata

import fi.metatavu.oss.api.impl.devices.DeviceController
import fi.metatavu.oss.api.impl.devicesurveys.DeviceSurveyEntity
import fi.metatavu.oss.api.impl.layouts.LayoutVariableRepository
import fi.metatavu.oss.api.impl.pages.PagePropertyRepository
import fi.metatavu.oss.api.impl.pages.PagesController
import fi.metatavu.oss.api.impl.pages.questions.PageQuestionController
import fi.metatavu.oss.api.impl.pages.questions.PageQuestionTranslator
import fi.metatavu.oss.api.impl.surveys.SurveyController
import fi.metatavu.oss.api.impl.translate.AbstractTranslator
import fi.metatavu.oss.api.model.DeviceSurveyData
import fi.metatavu.oss.api.model.DeviceSurveyPageData
import fi.metatavu.oss.api.model.PageProperty
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Translates DeviceSurvey into device survey data object
 */
@ApplicationScoped
class DeviceSurveyDataTranslator : AbstractTranslator<DeviceSurveyEntity, DeviceSurveyData>() {

    @Inject
    lateinit var surveyController: SurveyController

    @Inject
    lateinit var deviceController: DeviceController

    @Inject
    lateinit var pagesController: PagesController

    @Inject
    lateinit var pagePropertyRepository: PagePropertyRepository

    @Inject
    lateinit var layoutVariableRepository: LayoutVariableRepository

    @Inject
    lateinit var pageQuestionTranslator: PageQuestionTranslator

    @Inject
    lateinit var pageQuestionController: PageQuestionController

    override suspend fun translate(entity: DeviceSurveyEntity): DeviceSurveyData {
        val supportRichText = deviceController.supportsHtml(device = entity.device)
        val survey = surveyController.findSurvey(entity.survey.id) ?: throw IllegalArgumentException("Survey not found")
        val (pages) = pagesController.listPages(survey)
        return DeviceSurveyData(
            id = entity.id,
            deviceId = entity.device.id,
            surveyId = entity.survey.id,
            status = entity.status,
            publishStartTime = entity.publishStartTime,
            publishEndTime = entity.publishEndTime,
            title = entity.survey.title,
            timeout = entity.survey.timeout,
            pages = pages.map { page ->
                DeviceSurveyPageData(
                    id = page.id,
                    metadata = translateMetadata(page),
                    layoutHtml = processHtmlLayout(html = page.layout.html, supportRichText = supportRichText),
                    pageNumber = page.orderNumber,
                    properties = pagePropertyRepository.listByPage(
                        entity = page
                    ).map { prop ->
                        PageProperty(
                            key = prop.propertyKey,
                            value = processHtmlText(html = prop.value, supportRichText = supportRichText),
                        )
                    },
                    layoutVariables = layoutVariableRepository.listByLayout(page.layout).map { layoutVar ->
                        fi.metatavu.oss.api.model.LayoutVariable(
                            key = layoutVar.variablekey,
                            type = layoutVar.variabletype
                        )
                    },
                    question = pageQuestionController.find(page)?.let { pageQuestion ->
                        pageQuestionTranslator.translate(entity = pageQuestion, supportRichText = supportRichText)
                    },
                    nextButtonVisible = page.nextButtonVisible
                )
            },
            metadata = translateMetadata(entity)
        )
    }

    /**
     * Processes html layout for devices without rich text support
     *
     * @param html html to process
     * @param supportRichText should rich text be supported
     * @return processed html
     */
    private fun processHtmlLayout(html: String, supportRichText: Boolean): String {
        return if (supportRichText) {
            html
        } else {
            val titleRegex = Regex("<div id='([^']*)' data-component='header-container'>(.*?)</div>")
            val paragraphRegex = Regex("<div data-component='text-container' id='([^']*)'>(.*?)</div>")

            html
                .replace("\n", "")
                .replace(titleRegex) { matchResult ->
                    val id = matchResult.groupValues[1]
                    "<h1 id='$id' class='title md'></h1>"
                }.replace(paragraphRegex) { matchResult ->
                    val id = matchResult.groupValues[1]
                    "<p id='$id'></p>"
                }
        }
    }

}