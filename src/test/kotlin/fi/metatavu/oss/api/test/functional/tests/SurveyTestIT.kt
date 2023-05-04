package fi.metatavu.oss.api.test.functional.tests

import fi.metatavu.oss.api.test.functional.resources.LocalTestProfile
import fi.metatavu.oss.test.client.models.Survey
import fi.metatavu.oss.test.client.models.SurveyStatus
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
            val survey = testBuilder.manager.surveys.create(
                Survey(
                    title = "test survey",
                    description = "test survey description",
                    status = SurveyStatus.DRAFT,
                    timeout = 60,
                )
            )
            assertEquals("test survey", survey.title)
            assertEquals("test survey description", survey.description)
            assertEquals(60, survey.timeout)
            assertNotNull(survey.id)
            assertNotNull(survey.metadata!!.createdAt)
            assertNotNull(survey.metadata.creatorId)
            assertNotNull(survey.metadata.modifiedAt)
            assertNotNull(survey.metadata.lastModifierId)

            //permissions
            testBuilder.consumer.surveys.assertCreateFail(
                expectedStatus = 403,
                survey = Survey(title = "default survey", status = SurveyStatus.DRAFT, timeout = 60)
            )
            testBuilder.empty.surveys.assertCreateFail(
                expectedStatus = 401,
                survey = Survey(title = "default survey", status = SurveyStatus.DRAFT, timeout = 60)
            )
            testBuilder.notvalid.surveys.assertCreateFail(
                expectedStatus = 401,
                survey = Survey(title = "default survey", status = SurveyStatus.DRAFT, timeout = 60)
            )

            // Cannot create Survey with negative timeout
            testBuilder.manager.surveys.assertCreateFail(
                expectedStatus = 400,
                survey = Survey(title = "default survey", status = SurveyStatus.DRAFT, timeout = -1)
            )

            // Cannot create Survey with 0 timeout
            testBuilder.manager.surveys.assertCreateFail(
                expectedStatus = 400,
                survey = Survey(title = "default survey", status = SurveyStatus.DRAFT, timeout = 0)
            )
        }
    }

    @Test
    fun testListSurveys() {
        createTestBuilder().use { testBuilder ->
            val createdSurvey = testBuilder.manager.surveys.createDefault()
            testBuilder.manager.surveys.createDefault()
            testBuilder.manager.surveys.createDefault()
            val surveys = testBuilder.manager.surveys.list(null, null, null)
            assertEquals(3, surveys.size)
            val firstPage = testBuilder.manager.surveys.list(0, 1, null)
            assertEquals(1, firstPage.size)

            testBuilder.manager.surveys.update(
                surveyId = createdSurvey.id!!,
                newSurvey = createdSurvey.copy(status = SurveyStatus.APPROVED)
            )

            val approvedSurveys = testBuilder.manager.surveys.list(null, null, SurveyStatus.APPROVED)
            val draftSurveys = testBuilder.manager.surveys.list(null, null, SurveyStatus.DRAFT)
            assertEquals(1, approvedSurveys.size)
            assertEquals(2, draftSurveys.size)

            //permissions
            testBuilder.empty.surveys.assertListFail(401)
            testBuilder.notvalid.surveys.assertListFail(401)
        }
    }

    @Test
    fun testFindSurvey() {
        createTestBuilder().use { testBuilder ->
            val survey = testBuilder.manager.surveys.createDefault()
            val foundSurvey = testBuilder.manager.surveys.find(survey.id!!)
            assertEquals(survey.id, foundSurvey.id)
            assertEquals(survey.title, foundSurvey.title)

            //permissions
            testBuilder.empty.surveys.assertFindFail(401, survey.id)
            testBuilder.notvalid.surveys.assertFindFail(401, survey.id)
        }
    }

    @Test
    fun testUpdateSurvey() {
        createTestBuilder().use { testBuilder ->
            val survey = testBuilder.manager.surveys.createDefault()
            val surveyUpdateData = Survey(
                title = "updated survey",
                status = SurveyStatus.DRAFT,
                timeout = 600,
                description = "updated survey description"
            )
            val updatedSurvey = testBuilder.manager.surveys.update(survey.id!!, surveyUpdateData)
            assertEquals(survey.id, updatedSurvey.id)
            assertEquals("updated survey", updatedSurvey.title)
            assertEquals(600, updatedSurvey.timeout)
            assertEquals("updated survey description", updatedSurvey.description)

            //permissions
            testBuilder.consumer.surveys.assertUpdateFail(403, survey.id, surveyUpdateData)
            testBuilder.empty.surveys.assertUpdateFail(401, survey.id, surveyUpdateData)
            testBuilder.notvalid.surveys.assertUpdateFail(401, survey.id, surveyUpdateData)

            // Cannot update Survey with negative timeout
            testBuilder.manager.surveys.assertUpdateFail(
                expectedStatus = 400,
                surveyId = survey.id,
                newSurvey = updatedSurvey.copy(timeout = -1)
            )

            // Cannot update Survey with 0 timeout
            testBuilder.manager.surveys.assertUpdateFail(
                expectedStatus = 400,
                surveyId = survey.id,
                newSurvey = updatedSurvey.copy(timeout = 0)
            )
        }
    }

    @Test
    fun testDeleteSurvey() {
        createTestBuilder().use { testBuilder ->
            val survey = testBuilder.manager.surveys.createDefault()

            //permissions
            testBuilder.consumer.surveys.assertDeleteFail(403, survey.id!!)
            testBuilder.empty.surveys.assertDeleteFail(401, survey.id)
            testBuilder.notvalid.surveys.assertDeleteFail(401, survey.id)

            testBuilder.manager.surveys.delete(survey.id)
            testBuilder.manager.surveys.assertFindFail(404, survey.id)
        }
    }

}