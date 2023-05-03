package fi.metatavu.oss.api.test.functional.tests

import fi.metatavu.oss.api.test.functional.resources.LocalTestProfile
import fi.metatavu.oss.test.client.models.Page
import fi.metatavu.oss.test.client.models.PageProperty
import fi.metatavu.oss.test.client.models.PagePropertyType
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Tests for Pages API
 */
@QuarkusTest
@TestProfile(LocalTestProfile::class)
class PageTestIT: AbstractResourceTest() {

    private val page = Page(
        title = "title",
        html = "<html></html>",
        properties = arrayOf(
            PageProperty(key = "key", value = "value", type = PagePropertyType.TEXT),
            PageProperty(key = "key2", value = "value2", type = PagePropertyType.IMAGE_URL)
        )
    )

    @Test
    fun testListPages() {
        createTestBuilder().use {
            val survey = it.manager.surveys.createDefault()
            val survey2 = it.manager.surveys.createDefault()

            it.manager.pages.createDefault(survey.id!!)
            it.manager.pages.createDefault(survey.id)
            it.manager.pages.createDefault(survey.id)

            it.manager.pages.createDefault(survey2.id!!)
            it.manager.pages.createDefault(survey2.id)

            assertEquals(3, it.manager.pages.list(survey.id).size)
            assertEquals(2, it.manager.pages.list(survey2.id).size)

            //permissions
            it.notvalid.pages.assertListFail(surveyId = survey.id, expectedStatus = 401)
            it.empty.pages.assertListFail(surveyId = survey.id, expectedStatus = 401)

            it.manager.pages.assertListFail(surveyId = UUID.randomUUID(), expectedStatus = 404)
        }
    }

    @Test
    fun testCreatePageWithProperties() {
        createTestBuilder().use {
            val survey = it.manager.surveys.createDefault()

            val created = it.manager.pages.create(
                surveyId = survey.id!!,
                page = page
            )

            assertNotNull(created!!.id)
            assertEquals(page.title, created.title)
            assertEquals(page.html, created.html)
            assertEquals(2, created.properties?.size)
            val textProp = created.properties!!.find { prop -> prop.key == "key" }
            assertEquals(page.properties!![0].key, textProp!!.key)
            assertEquals(page.properties!![0].value, textProp.value)
            assertEquals(page.properties!![0].type, textProp.type)

            //permissions
            it.consumer.pages.assertCreateFail(surveyId = survey.id, expectedStatus = 403)
            it.notvalid.pages.assertCreateFail(surveyId = survey.id, expectedStatus = 401)
            it.empty.pages.assertCreateFail(surveyId = survey.id, expectedStatus = 401)

            it.manager.pages.assertCreateFail(UUID.randomUUID(), expectedStatus = 404)
        }
    }

    @Test
    fun testFindPage() {
        createTestBuilder().use {
            val survey = it.manager.surveys.createDefault()
            val survey2 = it.manager.surveys.createDefault()

            val createdPage = it.manager.pages.createDefault(surveyId = survey.id!!)
            val foundPage = it.manager.pages.find(surveyId = survey.id, pageId = createdPage.id!!)
            assertNotNull(foundPage.id)

            // permissions
            it.notvalid.pages.assertFindFail(surveyId = survey.id, pageId = createdPage.id!!, expectedStatus = 401)
            it.empty.pages.assertFindFail(surveyId = survey.id, pageId = createdPage.id!!, expectedStatus = 401)

            it.manager.pages.assertFindFail(surveyId = UUID.randomUUID(), pageId = createdPage.id!!, expectedStatus = 404)
            it.manager.pages.assertFindFail(surveyId = survey.id, pageId = UUID.randomUUID(), expectedStatus = 404)
            it.manager.pages.assertFindFail(surveyId = survey2.id!!, pageId = createdPage.id!!, expectedStatus = 404)
        }
    }

    @Test
    fun testUpdatePage() {
        createTestBuilder().use {
            val survey = it.manager.surveys.createDefault()
            val survey2 = it.manager.surveys.createDefault()

            val createdPage = it.manager.pages.createDefault(surveyId = survey.id!!)

            val updatedPage = it.manager.pages.update(
                surveyId = survey.id,
                pageId = createdPage.id!!,
                page = page
            )

            assertNotNull(updatedPage!!.id)
            assertEquals(page.title, updatedPage.title)
            assertEquals(page.html, updatedPage.html)
            assertEquals(2, updatedPage.properties?.size)
            val textProp = updatedPage.properties!!.find { prop -> prop.key == "key" }
            assertEquals(page.properties!![0].key, textProp!!.key)
            assertEquals(page.properties!![0].value, textProp.value)
            assertEquals(page.properties!![0].type, textProp.type)

            // permissions
            it.empty.pages.assertUpdateFail(surveyId = survey.id, pageId = createdPage.id, expectedStatus = 401)
            it.notvalid.pages.assertUpdateFail(surveyId = survey.id, pageId = createdPage.id, expectedStatus = 401)
            it.consumer.pages.assertUpdateFail(surveyId = survey.id, pageId = createdPage.id, expectedStatus = 403)

            it.manager.pages.assertUpdateFail(surveyId = UUID.randomUUID(), pageId = createdPage.id, expectedStatus = 404)
            it.manager.pages.assertUpdateFail(surveyId = survey.id, pageId = UUID.randomUUID(), expectedStatus = 404)
            it.manager.pages.assertUpdateFail(surveyId = survey2.id!!, pageId = createdPage.id, expectedStatus = 404)
        }
    }

    @Test
    fun testDeletePage() {
        createTestBuilder().use {
            val survey = it.manager.surveys.createDefault()
            val survey2 = it.manager.surveys.createDefault()

            var createdPage = it.manager.pages.createDefault(surveyId = survey.id!!)
            var pages = it.manager.pages.list(surveyId = survey.id)
            assertEquals(1, pages.size)

            it.manager.pages.delete(surveyId = survey.id, pageId = createdPage.id!!)
            pages = it.manager.pages.list(surveyId = survey.id)
            assertEquals(0, pages.size)

            // permissions
            it.empty.pages.assertDeleteFail(surveyId = survey.id, pageId = createdPage.id!!, expectedStatus = 401)
            it.notvalid.pages.assertDeleteFail(surveyId = survey.id, pageId = createdPage.id!!, expectedStatus = 401)
            it.consumer.pages.assertDeleteFail(surveyId = survey.id, pageId = createdPage.id!!, expectedStatus = 403)

            createdPage = it.manager.pages.createDefault(surveyId = survey.id)
            it.manager.pages.assertDeleteFail(surveyId = UUID.randomUUID(), pageId = createdPage.id!!, expectedStatus = 404)
            it.manager.pages.assertDeleteFail(surveyId = survey.id, pageId = UUID.randomUUID(), expectedStatus = 404)
            it.manager.pages.assertDeleteFail(surveyId = survey2.id!!, pageId = createdPage.id!!, expectedStatus = 404)
        }
    }
}
