package fi.metatavu.oss.api.test.functional.tests

import fi.metatavu.oss.api.test.functional.mqtt.TestMqttClient
import fi.metatavu.oss.api.test.functional.resources.LocalTestProfile
import fi.metatavu.oss.test.client.models.*
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
            assertEquals("default survey", survey.title)
            assertNotNull(survey.id)
            assertNotNull(survey.metadata!!.createdAt)
            assertNotNull(survey.metadata.creatorId)
            assertNotNull(survey.metadata.modifiedAt)
            assertNotNull(survey.metadata.lastModifierId)

            //permissions
            testBuilder.consumer.surveys.assertCreateFail(403, Survey(title = "default survey", status = SurveyStatus.DRAFT))
            testBuilder.empty.surveys.assertCreateFail(401, Survey(title = "default survey", status = SurveyStatus.DRAFT))
            testBuilder.notvalid.surveys.assertCreateFail(401, Survey(title = "default survey", status = SurveyStatus.DRAFT))
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
            val surveyUpdateData = Survey(title = "updated survey", status = SurveyStatus.DRAFT)
            val updatedSurvey = testBuilder.manager.surveys.update(survey.id!!, surveyUpdateData)
            assertEquals(survey.id, updatedSurvey.id)
            assertEquals("updated survey", updatedSurvey.title)

            //permissions
            testBuilder.consumer.surveys.assertUpdateFail(403, survey.id, surveyUpdateData)
            testBuilder.empty.surveys.assertUpdateFail(401, survey.id, surveyUpdateData)
            testBuilder.notvalid.surveys.assertUpdateFail(401, survey.id, surveyUpdateData)
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

    @Test
    fun testUpdateSurveyNotifications() {
        createTestBuilder().use { testBuilder ->
            val mqttClient = TestMqttClient()
            val (deviceId) = testBuilder.manager.deviceSurveys.setupTestDevice()
            val createdSurvey = testBuilder.manager.surveys.createDefault()
            testBuilder.manager.surveys.update(
                surveyId = createdSurvey.id!!,
                newSurvey = createdSurvey.copy(status = SurveyStatus.APPROVED)
            )
            val createdDeviceSurvey = testBuilder.manager.deviceSurveys.create(
                deviceId = deviceId,
                deviceSurvey = DeviceSurvey(
                    deviceId = deviceId,
                    surveyId = createdSurvey.id,
                    status = DeviceSurveyStatus.PUBLISHED
                )
            )

            val subscription = mqttClient.subscribe(
                targetClass = DeviceSurveyMessage::class.java,
                subscriptionTopic = "$deviceId/surveys/update"
            )

            testBuilder.manager.surveys.update(
                surveyId = createdSurvey.id,
                newSurvey = createdSurvey.copy(title = "updated-title")
            )

            val message = subscription.getMessages(1)

            assertEquals(1, message.size)
            assertEquals(deviceId, message[0].deviceId)
            assertEquals(createdDeviceSurvey.id, message[0].deviceSurveyId)
            assertEquals(DeviceSurveysMessageAction.UPDATE, message[0].action)
        }
    }
}