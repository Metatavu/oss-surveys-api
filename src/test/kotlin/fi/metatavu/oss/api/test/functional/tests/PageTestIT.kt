package fi.metatavu.oss.api.test.functional.tests

import fi.metatavu.oss.api.test.functional.resources.LocalTestProfile
import fi.metatavu.oss.test.client.models.*
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.*

/**
 * Tests for Pages API
 */
@QuarkusTest
@TestProfile(LocalTestProfile::class)
class PageTestIT: AbstractResourceTest() {

    @Test
    fun testListPages() = createTestBuilder().use {
        val survey = it.manager.surveys.createDefault()
        val survey2 = it.manager.surveys.createDefault()
        val layout = it.manager.layouts.createDefault()

        it.manager.pages.createDefault(surveyId = survey.id!!, layoutId = layout.id!!)
        it.manager.pages.createDefault(surveyId = survey.id, layoutId = layout.id)
        it.manager.pages.createDefault(surveyId = survey.id, layoutId = layout.id)

        it.manager.pages.createDefault(survey2.id!!, layoutId = layout.id)
        it.manager.pages.createDefault(survey2.id, layoutId = layout.id)

        assertEquals(3, it.manager.pages.list(survey.id).size)
        assertEquals(2, it.manager.pages.list(survey2.id).size)

        //permissions
        it.notvalid.pages.assertListFail(surveyId = survey.id, expectedStatus = 401)
        it.empty.pages.assertListFail(surveyId = survey.id, expectedStatus = 401)

        it.manager.pages.assertListFail(surveyId = UUID.randomUUID(), expectedStatus = 404)
    }

    @Test
    fun testCreatePageWithProperties() = createTestBuilder().use {
        val survey = it.manager.surveys.createDefault()
        val layout = it.manager.layouts.createDefault()
        val page = getTestPage(layoutId = layout.id!!)

        val created = it.manager.pages.create(
            surveyId = survey.id!!,
            page = page
        )

        assertNotNull(created!!.id)
        assertEquals(page.title, created.title)
        assertEquals(2, created.properties?.size)
        val textProp = created.properties!!.find { prop -> prop.key == "key" }
        assertEquals(page.properties?.get(0)?.key, textProp!!.key)
        assertEquals(page.properties?.get(0)?.value, textProp.value)

        //permissions
        it.consumer.pages.assertCreateFail(surveyId = survey.id, layoutId = layout.id, expectedStatus = 403)
        it.notvalid.pages.assertCreateFail(surveyId = survey.id, layoutId = layout.id, expectedStatus = 401)
        it.empty.pages.assertCreateFail(surveyId = survey.id, layoutId = layout.id, expectedStatus = 401)

        it.manager.pages.assertCreateFail(UUID.randomUUID(), layoutId = layout.id, expectedStatus = 404)
    }

    @Test
    fun createUpdatePageWithQuestion() = createTestBuilder().use { tb ->
        val survey = tb.manager.surveys.createDefault()
        val layout = tb.manager.layouts.createDefault()
        val page = getTestPage(layoutId = layout.id!!).copy(
            question = PageQuestion(
                type = PageQuestionType.SINGLE_SELECT,
                options = arrayOf(
                    PageQuestionOption(
                        questionOptionValue = "Heinz",
                        orderNumber = 0
                    ),
                    PageQuestionOption(
                        questionOptionValue = "Other",
                        orderNumber = 1
                    )
                )
            )
        )

        // test creating page with question
        val createdPage = tb.manager.pages.create(surveyId = survey.id!!, page = page)

        assertNotNull(createdPage!!.id)
        assertEquals(page.question!!.type, createdPage.question!!.type)
        assertEquals(2, createdPage.question.options.size)
        assertEquals("Heinz", createdPage.question.options[0].questionOptionValue)
        assertEquals(0, createdPage.question.options[0].orderNumber)
        assertEquals("Other", createdPage.question.options[1].questionOptionValue)
        assertEquals(1, createdPage.question.options[1].orderNumber)

        // test modifying the question
        val questionUpdateData = PageQuestion(
            type = PageQuestionType.MULTI_SELECT,
            options = arrayOf(
                PageQuestionOption(
                    questionOptionValue = "Heinz",
                    orderNumber = 0
                ),
                PageQuestionOption(
                    questionOptionValue = "Other",
                    orderNumber = 1
                ),
                PageQuestionOption(
                    questionOptionValue = "Hunts",
                    orderNumber = 2
                )
            )
        )

        val updatedPage = tb.manager.pages.update(surveyId = survey.id, pageId = createdPage.id!!, page = page.copy(question = questionUpdateData))
        assertNotNull(updatedPage.id)
        val updatedQuestion = updatedPage.question!!
        assertEquals(questionUpdateData.type, updatedQuestion.type)
        assertEquals(3, updatedQuestion.options.size)
        assertNotNull(updatedQuestion.options.find { it.questionOptionValue == "Heinz" })
        assertNotNull(updatedQuestion.options.find { it.questionOptionValue == "Other" })
        assertNotNull(updatedQuestion.options.find { it.questionOptionValue == "Hunts" })
    }

    @Test
    fun testFindPage() = createTestBuilder().use {
        val survey = it.manager.surveys.createDefault()
        val survey2 = it.manager.surveys.createDefault()
        val layout = it.manager.layouts.createDefault()

        val createdPage = it.manager.pages.createDefault(surveyId = survey.id!!, layoutId = layout.id!!)
        val foundPage = it.manager.pages.find(surveyId = survey.id, pageId = createdPage.id!!)
        assertNotNull(foundPage.id)

        // permissions
        it.notvalid.pages.assertFindFail(surveyId = survey.id, pageId = createdPage.id, expectedStatus = 401)
        it.empty.pages.assertFindFail(surveyId = survey.id, pageId = createdPage.id, expectedStatus = 401)

        it.manager.pages.assertFindFail(surveyId = UUID.randomUUID(), pageId = createdPage.id, expectedStatus = 404)
        it.manager.pages.assertFindFail(surveyId = survey.id, pageId = UUID.randomUUID(), expectedStatus = 404)
        it.manager.pages.assertFindFail(surveyId = survey2.id!!, pageId = createdPage.id, expectedStatus = 404)
    }

    @Test
    fun testUpdatePage() = createTestBuilder().use {
        val (deviceId, _) = it.manager.devices.setupTestDevice()
        val survey = it.manager.surveys.createDefault()
        approveSurvey(survey)
        val survey2 = it.manager.surveys.createDefault()
        val layout = it.manager.layouts.createDefault()
        val page = getTestPage(layoutId = layout.id!!)
        val createdPage = it.manager.pages.createDefault(surveyId = survey.id!!, layoutId = layout.id)

        val updatedPage = it.manager.pages.update(
            surveyId = survey.id,
            pageId = createdPage.id!!,
            page = page
        )

        assertNotNull(updatedPage.id)
        assertEquals(page.title, updatedPage.title)
        assertEquals(2, updatedPage.properties?.size)
        val textProp = updatedPage.properties!!.find { prop -> prop.key == "key" }
        assertEquals(page.properties?.get(0)?.key, textProp!!.key)
        assertEquals(page.properties?.get(0)?.value, textProp.value)

        // permissions
        it.empty.pages.assertUpdateFail(surveyId = survey.id, pageId = createdPage.id, layoutId = layout.id, expectedStatus = 401)
        it.notvalid.pages.assertUpdateFail(surveyId = survey.id, pageId = createdPage.id, layoutId = layout.id, expectedStatus = 401)
        it.consumer.pages.assertUpdateFail(surveyId = survey.id, pageId = createdPage.id, layoutId = layout.id, expectedStatus = 403)

        it.manager.pages.assertUpdateFail(surveyId = UUID.randomUUID(), pageId = createdPage.id, layoutId = layout.id, expectedStatus = 404)
        it.manager.pages.assertUpdateFail(surveyId = survey.id, pageId = UUID.randomUUID(), layoutId = layout.id, expectedStatus = 404)
        it.manager.pages.assertUpdateFail(surveyId = survey2.id!!, pageId = createdPage.id, layoutId = layout.id, expectedStatus = 404)

        //check that cannot be updated after publishing
        it.manager.deviceSurveys.create(
            deviceId = deviceId,
            deviceSurvey = DeviceSurvey(
                surveyId = survey.id,
                deviceId = deviceId,
                status = DeviceSurveyStatus.PUBLISHED,
                publishStartTime = OffsetDateTime.now().minusDays(5).toString(),
                publishEndTime = OffsetDateTime.now().minusDays(1).toString()
            )
        )
        it.manager.pages.assertUpdateFail(surveyId = survey.id, pageId = createdPage.id, layoutId = layout.id, expectedStatus = 400)
    }

    @Test
    fun testDeletePage() = createTestBuilder().use {
        val survey = it.manager.surveys.createDefault()
        val survey2 = it.manager.surveys.createDefault()
        val layout = it.manager.layouts.createDefault()

        var createdPage = it.manager.pages.createDefault(surveyId = survey.id!!, layoutId = layout.id!!)
        var pages = it.manager.pages.list(surveyId = survey.id)
        assertEquals(1, pages.size)

        it.manager.pages.delete(surveyId = survey.id, pageId = createdPage.id!!)
        pages = it.manager.pages.list(surveyId = survey.id)
        assertEquals(0, pages.size)

        // permissions
        it.empty.pages.assertDeleteFail(surveyId = survey.id, pageId = createdPage.id!!, expectedStatus = 401)
        it.notvalid.pages.assertDeleteFail(surveyId = survey.id, pageId = createdPage.id!!, expectedStatus = 401)
        it.consumer.pages.assertDeleteFail(surveyId = survey.id, pageId = createdPage.id!!, expectedStatus = 403)

        createdPage = it.manager.pages.createDefault(surveyId = survey.id, layoutId = layout.id)
        it.manager.pages.assertDeleteFail(surveyId = UUID.randomUUID(), pageId = createdPage.id!!, expectedStatus = 404)
        it.manager.pages.assertDeleteFail(surveyId = survey.id, pageId = UUID.randomUUID(), expectedStatus = 404)
        it.manager.pages.assertDeleteFail(surveyId = survey2.id!!, pageId = createdPage.id!!, expectedStatus = 404)
    }

    /**
     * Returns a test page
     *
     * @param layoutId layout id
     * @return test page
     */
    private fun getTestPage(layoutId: UUID): Page {
        return Page(
            title = "title",
            properties = arrayOf(
                PageProperty(key = "key", value = "value"),
                PageProperty(key = "key2", value = "value2")
            ),
            orderNumber = 1,
            layoutId = layoutId
        )
    }
}
