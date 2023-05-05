package fi.metatavu.oss.api.test.functional.tests

import fi.metatavu.oss.api.test.functional.resources.LocalTestProfile
import fi.metatavu.oss.test.client.models.Layout
import fi.metatavu.oss.test.client.models.Page
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Tests for Layouts API
 */
@QuarkusTest
@TestProfile(LocalTestProfile::class)
class LayoutTestIT: AbstractResourceTest() {

    @Test
    fun testListLayouts() {
        createTestBuilder().use {
            val l1 = it.manager.layouts.createDefault()
            it.manager.layouts.create(l1.copy(name = "1"))
            it.manager.layouts.create(l1.copy(name = "2"))

            assertEquals(0, it.manager.layouts.list(firstResult = 3, maxResults = 2).size)
            assertEquals(1, it.manager.layouts.list(firstResult = 2, maxResults = 2).size)
            assertEquals(2, it.manager.layouts.list(firstResult = 0, maxResults = 2).size)
            assertEquals(0, it.manager.layouts.list(firstResult = 0, maxResults = 0).size)
            assertEquals(3, it.manager.layouts.list(firstResult = null, maxResults = null).size)

            //permissions
            it.notvalid.layouts.assertListFail(expectedStatus = 401)
            it.empty.layouts.assertListFail(expectedStatus = 401)
        }
    }

    @Test
    fun testCreateLayout() {
        createTestBuilder().use {
            val layout = Layout(
                name = "name",
                thumbnail = "thumbnail",
                html = "<html></html>"
            )

            val created = it.manager.layouts.create(
                layout = layout
            )

            assertEquals(layout.name, created.name)
            assertEquals(layout.thumbnail, created.thumbnail)
            assertEquals(layout.html, created.html)
            assertNotNull(created.id)
            assertNotNull(created.metadata)

            //permissions
            it.notvalid.layouts.assertCreateFail(layout = layout, expectedStatus = 401)
            it.empty.layouts.assertCreateFail(layout = layout, expectedStatus = 401)
        }
    }

    @Test
    fun testFindLayout() {
        createTestBuilder().use {
            val created = it.manager.layouts.createDefault()

            val found = it.manager.layouts.find(created.id!!)

            assertEquals(created.name, found.name)
            assertEquals(created.thumbnail, found.thumbnail)
            assertEquals(created.html, found.html)
            assertNotNull(found.id)
            assertNotNull(found.metadata)

            //permissions
            it.notvalid.layouts.assertFindFail(layoutId = created.id, expectedStatus = 401)
            it.empty.layouts.assertFindFail(layoutId = created.id, expectedStatus = 401)

            it.manager.layouts.assertFindFail(layoutId = UUID.randomUUID(), expectedStatus = 404)
        }
    }

    @Test
    fun testUpdateLayout() {
        createTestBuilder().use {
            val created = it.manager.layouts.createDefault()

            val updated = it.manager.layouts.update(
                layoutId = created.id!!,
                layout = Layout(
                    name = "name2",
                    thumbnail = "thumbnail2",
                    html = "<html2></html2>"
                )
            )

            assertEquals("name2", updated.name)
            assertEquals("thumbnail2", updated.thumbnail)
            assertEquals("<html2></html2>", updated.html)
            assertNotNull(updated.id)
            assertNotNull(updated.metadata)

            //permissions
            it.notvalid.layouts.assertUpdateFail(layoutId = created.id, layout = updated, expectedStatus = 401)
            it.empty.layouts.assertUpdateFail(layoutId = created.id, layout = updated, expectedStatus = 401)

            it.manager.layouts.assertUpdateFail(layoutId = UUID.randomUUID(), layout = updated, expectedStatus = 404)
        }
    }

    @Test
    fun testDeleteLayout() {
        createTestBuilder().use {
            val layout = it.manager.layouts.createDefault()
            val survey = it.manager.surveys.createDefault()

            //permissions
            it.notvalid.layouts.assertDeleteFail(layoutId = layout.id!!, expectedStatus = 401)
            it.empty.layouts.assertDeleteFail(layoutId = layout.id, expectedStatus = 401)

            it.manager.layouts.assertDeleteFail(layoutId = UUID.randomUUID(), expectedStatus = 404)

            // cannot remove layout if there are pages using it
            it.manager.pages.createDefault(surveyId = survey.id!!, layoutId = layout.id)
            it.manager.layouts.assertDeleteFail(layoutId = layout.id, expectedStatus = 400)
            it.manager.pages.list(surveyId = survey.id).forEach { page: Page ->
                it.manager.pages.delete(survey.id, page.id!!)
            }

            assertEquals(1, it.manager.layouts.list(firstResult = null, maxResults = null).size)
            it.manager.layouts.delete(layout.id)
            assertEquals(0, it.manager.layouts.list(firstResult = null, maxResults = null).size)
        }
    }
}