package fi.metatavu.oss.api.test.functional.tests

import fi.metatavu.oss.api.test.functional.resources.LocalTestProfile
import fi.metatavu.oss.test.client.models.Survey
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Tests for surveys API
 */
@QuarkusTest
@TestProfile(LocalTestProfile::class)
class SurveyTestIT : AbstractResourceTest() {

    @Test
    fun testCreateSurvey() {
        createTestBuilder().use { testBuilder ->
            val survey = testBuilder.manager.surveys.createDefault()
            assertEquals("default survey", survey!!.title)
            assertNotNull(survey.id)
            assertNotNull(survey.metadata!!.createdAt)
            assertNotNull(survey.metadata.creatorId)
            assertNotNull(survey.metadata.modifiedAt)
            assertNotNull(survey.metadata.lastModifierId)

            //permissions
            testBuilder.consumer.surveys.assertCreateFail(403, Survey(title = "default survey"))
            testBuilder.empty.surveys.assertCreateFail(401, Survey(title = "default survey"))
            testBuilder.notvalid.surveys.assertCreateFail(401, Survey(title = "default survey"))
        }
    }

    @Test
    fun testListSurveys() {
        createTestBuilder().use { testBuilder ->
            testBuilder.manager.surveys.createDefault()
            testBuilder.manager.surveys.createDefault()
            testBuilder.manager.surveys.createDefault()
            val surveys = testBuilder.manager.surveys.list(null, null)
            assertEquals(3, surveys.size)
            val firstPage = testBuilder.manager.surveys.list(0, 1)
            assertEquals(1, firstPage.size)

            //permissions
            testBuilder.empty.surveys.assertListFail(401)
            testBuilder.notvalid.surveys.assertListFail(401)
        }
    }

    @Test
    fun testFindSurvey() {
        createTestBuilder().use { testBuilder ->
            val survey = testBuilder.manager.surveys.createDefault()
            val foundSurvey = testBuilder.manager.surveys.find(survey!!.id!!)
            assertEquals(survey.id, foundSurvey.id)
            assertEquals(survey.title, foundSurvey.title)

            //permissions
            testBuilder.empty.surveys.assertFindFail(401, survey.id!!)
            testBuilder.notvalid.surveys.assertFindFail(401, survey.id)
        }
    }

    @Test
    fun testUpdateSurvey() {
        createTestBuilder().use { testBuilder ->
            val survey = testBuilder.manager.surveys.createDefault()
            val surveyUpdateData = Survey(title = "updated survey")
            val updatedSurvey = testBuilder.manager.surveys.update(survey!!.id!!, surveyUpdateData)
            assertEquals(survey.id, updatedSurvey.id)
            assertEquals("updated survey", updatedSurvey.title)

            //permissions
            testBuilder.consumer.surveys.assertUpdateFail(403, survey.id!!, surveyUpdateData)
            testBuilder.empty.surveys.assertUpdateFail(401, survey.id, surveyUpdateData)
            testBuilder.notvalid.surveys.assertUpdateFail(401, survey.id, surveyUpdateData)
        }
    }

    @Test
    fun testDeleteSurvey() {
        createTestBuilder().use { testBuilder ->
            val survey = testBuilder.manager.surveys.createDefault()

            //permissions
            testBuilder.consumer.surveys.assertDeleteFail(403, survey!!.id!!)
            testBuilder.empty.surveys.assertDeleteFail(401, survey.id!!)
            testBuilder.notvalid.surveys.assertDeleteFail(401, survey.id)

            testBuilder.manager.surveys.delete(survey.id)
            testBuilder.manager.surveys.assertFindFail(404, survey.id)
        }
    }

}