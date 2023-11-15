package fi.metatavu.oss.api.test.functional.tests

import fi.metatavu.oss.api.test.functional.mqtt.TestMqttClient
import fi.metatavu.oss.api.test.functional.resources.LocalTestProfile
import fi.metatavu.oss.test.client.models.DeviceSurvey
import fi.metatavu.oss.test.client.models.DeviceSurveyMessage
import fi.metatavu.oss.test.client.models.DeviceSurveyStatus
import fi.metatavu.oss.test.client.models.SurveyStatus
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import org.awaitility.Awaitility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

/**
 * Tests for Scheduler
 */
@QuarkusTest
@TestProfile(LocalTestProfile::class)
class SchedulerTestIT: AbstractResourceTest() {

    @Test
    fun testScheduler() = createTestBuilder().use { testBuilder ->
        val mqttClient = TestMqttClient()
        val (deviceId) = testBuilder.manager.devices.setupTestDevice()
        val createSubscription = mqttClient.subscribe(
            targetClass = DeviceSurveyMessage::class.java,
            subscriptionTopic = "$deviceId/surveys/create"
        )
        val updateSubscription = mqttClient.subscribe(
            targetClass = DeviceSurveyMessage::class.java,
            subscriptionTopic = "$deviceId/surveys/update"
        )
        val deleteSubscription = mqttClient.subscribe(
            targetClass = DeviceSurveyMessage::class.java,
            subscriptionTopic = "$deviceId/surveys/delete"
        )
        val existingSurveys = testBuilder.manager.surveys.list()

        assertEquals(0, existingSurveys.size)

        val createdSurvey = testBuilder.manager.surveys.createDefault()
        val existingDeviceSurveys = testBuilder.manager.deviceSurveys.list()
        assertEquals(0, existingDeviceSurveys.size)
        testBuilder.manager.surveys.update(
            surveyId = createdSurvey.id!!,
            newSurvey = createdSurvey.copy(status = SurveyStatus.APPROVED)
        )

        // Create scheduled device survey to be scheduled in 10 seconds
        testBuilder.manager.deviceSurveys.create(
            addClosable = false,
            deviceId = deviceId,
            deviceSurvey = DeviceSurvey(
                deviceId = deviceId,
                surveyId = createdSurvey.id,
                status = DeviceSurveyStatus.SCHEDULED,
                publishStartTime = OffsetDateTime.now().plusSeconds(10).toString(),
                publishEndTime = OffsetDateTime.now().plusMinutes(1).toString()
            )
        )

        assertEquals(1, createSubscription.getMessages(1).size)

        // Assert that there's only the one just created device survey and it's status is scheduled
        val deviceSurveys1 = testBuilder.manager.deviceSurveys.list()
        assertEquals(1, deviceSurveys1.size)
        assertTrue(deviceSurveys1.all { it.status == DeviceSurveyStatus.SCHEDULED })

        // Wait until the earlier created device survey is published and that there's no scheduled device surveys
        Awaitility
            .await()
            .atMost(1, TimeUnit.MINUTES)
            .pollInterval(5, TimeUnit.SECONDS)
            .until {
                testBuilder.manager.deviceSurveys.list().all { it.status == DeviceSurveyStatus.PUBLISHED } &&
                testBuilder.manager.deviceSurveys.list().none { it.status == DeviceSurveyStatus.SCHEDULED }
            }

        assertEquals(1, updateSubscription.getMessages(1).size)

        // List all device surveys and assert that there's only the one published device survey
        val deviceSurveys2 = testBuilder.manager.deviceSurveys.list()
        assertEquals(1, deviceSurveys2.size)
        assertTrue(deviceSurveys2.all { it.status == DeviceSurveyStatus.PUBLISHED })

        // Create another device survey to be scheduled in 10 seconds
        val createdDeviceSurvey2 = testBuilder.manager.deviceSurveys.create(
            addClosable = false,
            deviceId = deviceId,
            deviceSurvey = DeviceSurvey(
                deviceId = deviceId,
                surveyId = createdSurvey.id,
                status = DeviceSurveyStatus.SCHEDULED,
                publishStartTime = OffsetDateTime.now().plusSeconds(10).toString(),
                publishEndTime = OffsetDateTime.now().plusMinutes(1).toString()
            )
        )

        assertEquals(2, createSubscription.getMessages(2).size)

        // Assert that there's two device surveys, one scheduled and one published
        val deviceSurveys3 = testBuilder.manager.deviceSurveys.list()
        val deviceSurveysPublished = deviceSurveys3.filter { it.status == DeviceSurveyStatus.PUBLISHED }
        val deviceSurveysScheduled = deviceSurveys3.filter { it.status == DeviceSurveyStatus.SCHEDULED }
        assertEquals(1, deviceSurveysPublished.size)
        assertEquals(1, deviceSurveysScheduled.size)
        assertEquals(2, deviceSurveys3.size)

        Awaitility
            .await()
            .atMost(1, TimeUnit.MINUTES)
            .until {
                testBuilder.manager.deviceSurveys.list().all { it.status == DeviceSurveyStatus.PUBLISHED } &&
                testBuilder.manager.deviceSurveys.list().none { it.status == DeviceSurveyStatus.SCHEDULED }
            }

        assertEquals(2, updateSubscription.getMessages(2).size)
        assertEquals(1, deleteSubscription.getMessages(1).size)

        // Clean the device surveys manually
        testBuilder.manager.deviceSurveys.delete(createdDeviceSurvey2.deviceId, createdDeviceSurvey2.id!!)
    }
}