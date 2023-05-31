package fi.metatavu.oss.api.test.functional.tests

import fi.metatavu.oss.api.test.functional.TestBuilder
import fi.metatavu.oss.api.test.functional.mqtt.TestMqttClient
import fi.metatavu.oss.api.test.functional.resources.LocalTestProfile
import fi.metatavu.oss.test.client.models.*
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * Tests for device surveys API
 */
@QuarkusTest
@TestProfile(LocalTestProfile::class)
class DeviceSurveysTestIT: AbstractResourceTest() {

    @Test
    fun testCreateDeviceSurvey() = createTestBuilder().use { testBuilder ->
        val (deviceId) = testBuilder.manager.devices.setupTestDevice()
        val createdSurvey = testBuilder.manager.surveys.createDefault()
        approveSurvey(createdSurvey)
        val deviceSurveyToCreate = DeviceSurvey(
            surveyId = createdSurvey.id!!,
            deviceId = deviceId,
            status = DeviceSurveyStatus.PUBLISHED,
        )
        val createdDeviceSurvey = testBuilder.manager.deviceSurveys.create(
            addClosable = false,
            deviceId = deviceId,
            deviceSurvey = deviceSurveyToCreate
        )

        assertNotNull(createdDeviceSurvey.id)
        assertEquals(createdDeviceSurvey.status, DeviceSurveyStatus.PUBLISHED)
        assertEquals(createdDeviceSurvey.surveyId, createdSurvey.id)
        assertEquals(createdDeviceSurvey.deviceId, deviceId)
        assertNotNull(createdDeviceSurvey.metadata!!.createdAt)
        assertNotNull(createdDeviceSurvey.metadata.creatorId)
        assertNotNull(createdDeviceSurvey.metadata.modifiedAt)
        assertNotNull(createdDeviceSurvey.metadata.lastModifierId)
        assertEquals(createdDeviceSurvey.metadata.creatorId, createdDeviceSurvey.metadata.lastModifierId)

        // Test that creating a new device survey when device has published surveys deletes the old ones
        val createdDeviceSurvey2 = testBuilder.manager.deviceSurveys.create(deviceId = deviceId, deviceSurvey = deviceSurveyToCreate)
        val foundDeviceSurveys = testBuilder.manager.deviceSurveys.list(deviceId)

        assertEquals(1, foundDeviceSurveys.size)
        assertEquals(createdDeviceSurvey2.id, foundDeviceSurveys[0].id)
        assertNotEquals(createdDeviceSurvey.id, foundDeviceSurveys[0].id)

        // permissions
        testBuilder.consumer.deviceSurveys.assertCreateFail(403, deviceId, deviceSurveyToCreate)
        testBuilder.empty.deviceSurveys.assertCreateFail(401, deviceId, deviceSurveyToCreate)
        testBuilder.notvalid.deviceSurveys.assertCreateFail(401, deviceId, deviceSurveyToCreate)
    }

    @Test
    fun testListDeviceSurveys() = createTestBuilder().use { testBuilder ->
        val (deviceId) = testBuilder.manager.devices.setupTestDevice()
        val (deviceId2) = testBuilder.manager.devices.setupTestDevice("321")
        val createdSurveys = mutableListOf<Survey>()

        val createdSurveyDevice2 = testBuilder.manager.surveys.create(
            survey = Survey(
                title = "device-2-test-survey",
                status = SurveyStatus.DRAFT,
                timeout = 60
            )
        )
        approveSurvey(createdSurveyDevice2)

        testBuilder.manager.deviceSurveys.create(
            deviceId = deviceId2,
            deviceSurvey = DeviceSurvey(
                surveyId = createdSurveyDevice2.id!!,
                deviceId = deviceId2,
                status = DeviceSurveyStatus.PUBLISHED
            )
        )

        for (i in 1..5) {
            val createdSurvey = testBuilder.manager.surveys.create(
                survey = Survey(
                    title = "test-survey-$i",
                    status = SurveyStatus.DRAFT,
                    timeout = 60
                )
            )
            testBuilder.manager.surveys.update(
                surveyId = createdSurvey.id!!,
                newSurvey = createdSurvey.copy(status = SurveyStatus.APPROVED)
            )
            testBuilder.manager.deviceSurveys.create(
                deviceId = deviceId,
                deviceSurvey = DeviceSurvey(
                    surveyId = createdSurvey.id,
                    deviceId = deviceId,
                    status = DeviceSurveyStatus.SCHEDULED,
                    publishStartTime = OffsetDateTime.now().plusDays(1).toString(),
                    publishEndTime = OffsetDateTime.now().plusDays(2).toString()
                )
            )
            createdSurveys.add(createdSurvey)
        }

        val deviceSurveys = testBuilder.manager.deviceSurveys.list(
            deviceId = deviceId,
            firstResult = null,
            maxResults = null
        )

        val device2Surveys = testBuilder.manager.deviceSurveys.list(
            deviceId = deviceId2,
            firstResult = null,
            maxResults = null
        )

        assertEquals(1, device2Surveys.size)
        assertEquals(5, deviceSurveys.size)
        deviceSurveys.forEach {
            assertEquals(it.deviceId, deviceId)
            assertNotNull(createdSurveys.find { survey -> survey.id == it.surveyId })
        }

        // Permissions
        testBuilder.consumer.deviceSurveys.assertListFail(403, deviceId)
        testBuilder.notvalid.deviceSurveys.assertListFail(401, deviceId)
        testBuilder.empty.deviceSurveys.assertListFail(401, deviceId)
    }

    @Test
    fun testFindDeviceSurvey() = createTestBuilder().use { testBuilder ->
        val (deviceId) = testBuilder.manager.devices.setupTestDevice()
        val createdSurvey = testBuilder.manager.surveys.createDefault()
        val deviceSurveyToCreate = DeviceSurvey(
            surveyId = createdSurvey.id!!,
            deviceId = deviceId,
            status = DeviceSurveyStatus.PUBLISHED
        )
        approveSurvey(createdSurvey)

        val createdDeviceSurvey1 = testBuilder.manager.deviceSurveys.create(
            deviceId = deviceId,
            deviceSurvey = deviceSurveyToCreate
        )
        val createdDeviceSurvey2 = testBuilder.manager.deviceSurveys.create(
            deviceId = deviceId,
            deviceSurvey = deviceSurveyToCreate.copy(
                status = DeviceSurveyStatus.SCHEDULED,
                publishStartTime = OffsetDateTime.now().plusDays(1).toString(),
                publishEndTime = OffsetDateTime.now().plusDays(2).toString()
            )
        )
        val foundDeviceSurvey1 = testBuilder.manager.deviceSurveys.find(
            deviceId = deviceId,
            deviceSurveyId = createdDeviceSurvey1.id!!
        )
        val foundDeviceSurvey2 = testBuilder.manager.deviceSurveys.find(
            deviceId = deviceId,
            deviceSurveyId = createdDeviceSurvey2.id!!
        )

        assertNotNull(foundDeviceSurvey1)
        assertEquals(foundDeviceSurvey1.surveyId, createdSurvey.id)
        assertEquals(foundDeviceSurvey1.deviceId, deviceId)
        assertEquals(foundDeviceSurvey1.status, DeviceSurveyStatus.PUBLISHED)

        assertNotNull(foundDeviceSurvey2)
        assertEquals(foundDeviceSurvey2.surveyId, createdSurvey.id)
        assertEquals(foundDeviceSurvey2.deviceId, deviceId)
        assertEquals(foundDeviceSurvey2.status, DeviceSurveyStatus.SCHEDULED)
    }

    @Test
    fun testUpdateDeviceSurvey() = createTestBuilder().use { testBuilder ->
        val (deviceId) = testBuilder.manager.devices.setupTestDevice()
        val createdSurvey = testBuilder.manager.surveys.createDefault()
        approveSurvey(createdSurvey)

        val createdDeviceSurvey = testBuilder.manager.deviceSurveys.create(
            addClosable = false,
            deviceId = deviceId,
            deviceSurvey = DeviceSurvey(
                surveyId = createdSurvey.id!!,
                deviceId = deviceId,
                status = DeviceSurveyStatus.PUBLISHED
            )
        )

        val updatedDeviceSurvey = testBuilder.manager.deviceSurveys.update(
            deviceId = deviceId,
            deviceSurveyId = createdDeviceSurvey.id!!,
            deviceSurvey = createdDeviceSurvey.copy(
                status = DeviceSurveyStatus.SCHEDULED,
                publishStartTime = OffsetDateTime.now().plusDays(1).toString(),
                publishEndTime = OffsetDateTime.now().plusDays(2).toString()
            )
        )

        assertEquals(createdDeviceSurvey.id, updatedDeviceSurvey.id)
        assertEquals(createdDeviceSurvey.deviceId, updatedDeviceSurvey.deviceId)
        assertEquals(createdDeviceSurvey.surveyId, updatedDeviceSurvey.surveyId)
        assertEquals(createdDeviceSurvey.status, DeviceSurveyStatus.PUBLISHED)
        assertEquals(updatedDeviceSurvey.status, DeviceSurveyStatus.SCHEDULED)

        // Test that updating a device survey to be published when device has published surveys deletes the old ones
        testBuilder.manager.deviceSurveys.update(
            deviceId = deviceId,
            deviceSurveyId = createdDeviceSurvey.id,
            deviceSurvey = createdDeviceSurvey.copy(
                status = DeviceSurveyStatus.PUBLISHED
            )
        )
        val foundDeviceSurveys1 = testBuilder.manager.deviceSurveys.list(deviceId)
        assertEquals(1, foundDeviceSurveys1.size)
        val createdDeviceSurvey2 = testBuilder.manager.deviceSurveys.create(deviceId = deviceId, deviceSurvey = createdDeviceSurvey)
        val foundDeviceSurveys2 = testBuilder.manager.deviceSurveys.list(deviceId)

        assertEquals(1, foundDeviceSurveys2.size)
        assertEquals(createdDeviceSurvey2.id, foundDeviceSurveys2[0].id)
    }

    @Test
    fun testUpdateDeviceSurveyFail() = createTestBuilder().use { testBuilder ->
        val (deviceId) = testBuilder.manager.devices.setupTestDevice()
        val createdSurvey = testBuilder.manager.surveys.createDefault()
        testBuilder.manager.surveys.update(
            surveyId = createdSurvey.id!!,
            newSurvey = createdSurvey.copy(status = SurveyStatus.APPROVED)
        )
        val deviceSurveyToCreate = DeviceSurvey(
            surveyId = createdSurvey.id,
            deviceId = deviceId,
            status = DeviceSurveyStatus.PUBLISHED
        )
        testBuilder.manager.deviceSurveys.create(
            deviceId = deviceId,
            deviceSurvey = deviceSurveyToCreate
        )
        testBuilder.manager.deviceSurveys.assertUpdateFail(
            expectedStatusCode = 400,
            deviceId = deviceId,
            deviceSurveyId = UUID.randomUUID(),
            deviceSurvey = deviceSurveyToCreate
        )
        testBuilder.manager.deviceSurveys.assertUpdateFail(
            expectedStatusCode = 400,
            deviceId = deviceId,
            deviceSurveyId = UUID.randomUUID(),
            deviceSurvey = deviceSurveyToCreate.copy(deviceId = UUID.randomUUID())
        )
    }

    @Test
    fun testInvalidScheduledDeviceSurvey() = createTestBuilder().use { testBuilder ->
        val (deviceId) = testBuilder.manager.devices.setupTestDevice()
        val createdSurvey = testBuilder.manager.surveys.createDefault()
        testBuilder.manager.surveys.update(
            surveyId = createdSurvey.id!!,
            newSurvey = createdSurvey.copy(status = SurveyStatus.APPROVED)
        )
        val deviceSurveyToCreate = DeviceSurvey(
            surveyId = createdSurvey.id,
            deviceId = deviceId,
            status = DeviceSurveyStatus.SCHEDULED
        )

        /// Missing publishStartTime and publishEndTime
        testBuilder.manager.deviceSurveys.assertCreateFail(
            expectedStatusCode = 400,
            deviceId = deviceId,
            deviceSurvey = deviceSurveyToCreate
        )
        /// Missing publishStartTime
        testBuilder.manager.deviceSurveys.assertCreateFail(
            expectedStatusCode = 400,
            deviceId = deviceId,
            deviceSurvey = deviceSurveyToCreate.copy(publishEndTime = OffsetDateTime.now().plusDays(1).toString())
        )
        /// Missing publishEndTime
        testBuilder.manager.deviceSurveys.assertCreateFail(
            expectedStatusCode = 400,
            deviceId = deviceId,
            deviceSurvey = deviceSurveyToCreate.copy(publishStartTime = OffsetDateTime.now().plusDays(1).toString())
        )
        /// publishStartTime is in the past
        testBuilder.manager.deviceSurveys.assertCreateFail(
            expectedStatusCode = 400,
            deviceId = deviceId,
            deviceSurvey = deviceSurveyToCreate.copy(
                publishStartTime = OffsetDateTime.now().minusDays(1).toString(),
                publishEndTime = OffsetDateTime.now().plusDays(1).toString()
            )
        )
        /// publishEndTime is before publishStartTime
        testBuilder.manager.deviceSurveys.assertCreateFail(
            expectedStatusCode = 400,
            deviceId = deviceId,
            deviceSurvey = deviceSurveyToCreate.copy(
                publishStartTime = OffsetDateTime.now().plusDays(1).toString(),
                publishEndTime = OffsetDateTime.now().toString()
            )
        )

        // Create Device Survey and try to update it with invalid schedule
        val createdDeviceSurvey = testBuilder.manager.deviceSurveys.create(
            deviceId = deviceId,
            deviceSurvey = deviceSurveyToCreate.copy(status = DeviceSurveyStatus.PUBLISHED)
        )

        testBuilder.manager.deviceSurveys.assertUpdateFail(
            expectedStatusCode = 400,
            deviceId = deviceId,
            deviceSurveyId = createdDeviceSurvey.id!!,
            deviceSurvey = createdDeviceSurvey.copy(
                status = DeviceSurveyStatus.SCHEDULED
            )
        )
    }

    @Test
    fun testDeleteDeviceSurvey() = createTestBuilder().use { testBuilder ->
        val (deviceId) = testBuilder.manager.devices.setupTestDevice()
        val createdSurvey = testBuilder.manager.surveys.createDefault()
        testBuilder.manager.surveys.update(
            surveyId = createdSurvey.id!!,
            newSurvey = createdSurvey.copy(status = SurveyStatus.APPROVED)
        )
        val createdDeviceSurvey = testBuilder.manager.deviceSurveys.create(
            deviceId = deviceId,
            DeviceSurvey(
                surveyId = createdSurvey.id,
                deviceId = deviceId,
                status = DeviceSurveyStatus.PUBLISHED
            )
        )
        val foundDeviceSurvey = testBuilder.manager.deviceSurveys.find(
            deviceId = deviceId,
            deviceSurveyId = createdDeviceSurvey.id!!
        )

        assertNotNull(foundDeviceSurvey)
        assertEquals(foundDeviceSurvey, createdDeviceSurvey)

        testBuilder.manager.deviceSurveys.delete(
            deviceId = deviceId,
            deviceSurveyId = createdDeviceSurvey.id
        )

        testBuilder.manager.deviceSurveys.assertFindFail(404, deviceId, createdDeviceSurvey.id)
    }

    @Test
    fun testCreateInvalidDeviceSurvey() = createTestBuilder().use { testBuilder ->
        val (deviceId) = testBuilder.manager.devices.setupTestDevice()
        val createdSurvey = testBuilder.manager.surveys.createDefault()
        val deviceSurveyToCreate = DeviceSurvey(
            surveyId = createdSurvey.id!!,
            deviceId = deviceId,
            status = DeviceSurveyStatus.PUBLISHED
        )

        // Device doesn't exist
        testBuilder.manager.deviceSurveys.assertCreateFail(
            expectedStatusCode = 404,
            deviceId = UUID.randomUUID(),
            deviceSurvey = deviceSurveyToCreate
        )
        // Survey is in DRAFT status
        testBuilder.manager.deviceSurveys.assertCreateFail(
            expectedStatusCode = 400,
            deviceId = deviceId,
            deviceSurvey = deviceSurveyToCreate
        )
        // Survey doesn't exist
        testBuilder.manager.deviceSurveys.assertCreateFail(
            expectedStatusCode = 400,
            deviceId = deviceId,
            deviceSurvey = deviceSurveyToCreate.copy(surveyId = UUID.randomUUID())
        )
        // Device id in path doesn't match
        testBuilder.manager.deviceSurveys.assertCreateFail(
            expectedStatusCode = 400,
            deviceId = deviceId,
            deviceSurvey = deviceSurveyToCreate.copy(deviceId = UUID.randomUUID())
        )
    }

    @Test
    fun testDeleteInvalidDeviceSurvey() = createTestBuilder().use { testBuilder ->
        val (deviceId) = testBuilder.manager.devices.setupTestDevice()
        // Device doesn't exist
        testBuilder.manager.deviceSurveys.assertDeleteFail(
            expectedStatusCode = 404,
            deviceId = UUID.randomUUID(),
            deviceSurveyId = UUID.randomUUID()
        )
        // Survey doesn't exist
        testBuilder.manager.deviceSurveys.assertDeleteFail(
            expectedStatusCode = 404,
            deviceId = deviceId,
            deviceSurveyId = UUID.randomUUID()
        )
    }

    @Test
    fun testFindDeviceSurveyFail() = createTestBuilder().use { testBuilder ->
        val (deviceId1) = testBuilder.manager.devices.setupTestDevice()

        // Device not found
        testBuilder.manager.deviceSurveys.assertFindFail(
            expectedStatusCode = 404,
            deviceId = deviceId1,
            deviceSurveyId = UUID.randomUUID()
        )

        // Device Survey not found
        testBuilder.manager.deviceSurveys.assertFindFail(
            expectedStatusCode = 404,
            deviceId = deviceId1,
            deviceSurveyId = UUID.randomUUID()
        )

        //not-manager roles
        testBuilder.empty.deviceSurveys.assertFindFail(
            401,
            deviceId1,
            UUID.randomUUID()
        )
        testBuilder.consumer.deviceSurveys.assertFindFail(
            403,
            deviceId1,
            UUID.randomUUID()
        )
    }

    @Test
    fun testDeviceSurveysRealtimeNotification() = createTestBuilder().use { testBuilder ->
        val (deviceId) = testBuilder.manager.devices.setupTestDevice()
        val createdSurvey = testBuilder.manager.surveys.createDefault()
        testBuilder.manager.surveys.update(
            surveyId = createdSurvey.id!!,
            newSurvey = createdSurvey.copy(status = SurveyStatus.APPROVED)
        )
        val mqttTopics = listOf(
            "$deviceId/surveys/create",
            "$deviceId/surveys/update",
            "$deviceId/surveys/delete"
        )
        val mqttClient = TestMqttClient()
        val (createSubscription, updateSubscription, deleteSubscription) = mqttTopics.map {
            mqttClient.subscribe(
                targetClass = DeviceSurveyMessage::class.java,
                subscriptionTopic = it
            )
        }
        val createdDeviceSurvey = testBuilder.manager.deviceSurveys.create(
            deviceId = deviceId,
            DeviceSurvey(
                surveyId = createdSurvey.id,
                deviceId = deviceId,
                status = DeviceSurveyStatus.PUBLISHED
            )
        )
        val createMessage = createSubscription.getMessages(1)
        testBuilder.manager.deviceSurveys.update(
            deviceId = deviceId,
            deviceSurveyId = createdDeviceSurvey.id!!,
            deviceSurvey = createdDeviceSurvey.copy(
                status = DeviceSurveyStatus.PUBLISHED,
            )
        )
        val updateMessage = updateSubscription.getMessages(1)
        testBuilder.manager.deviceSurveys.delete(
            deviceId = deviceId,
            deviceSurveyId = createdDeviceSurvey.id
        )
        val deleteMessage = deleteSubscription.getMessages(1)

        assertEquals(1, createMessage.size)
        assertEquals(1, updateMessage.size)
        assertEquals(1, deleteMessage.size)
        assertEquals(deviceId, createMessage[0].deviceId)
        assertEquals(createdDeviceSurvey.id, createMessage[0].deviceSurveyId)
        assertEquals(DeviceSurveysMessageAction.CREATE, createMessage[0].action)
        assertEquals(deviceId, updateMessage[0].deviceId)
        assertEquals(createdDeviceSurvey.id, updateMessage[0].deviceSurveyId)
        assertEquals(DeviceSurveysMessageAction.UPDATE, updateMessage[0].action)
        assertEquals(deviceId, updateMessage[0].deviceId)
        assertEquals(createdDeviceSurvey.id, deleteMessage[0].deviceSurveyId)
        assertEquals(DeviceSurveysMessageAction.DELETE, deleteMessage[0].action)
    }

    @Test
    fun testDeviceSurveyStatistics() = createTestBuilder().use { testBuilder ->
        val ( deviceSurvey, pages ) = setupStatisticsEnvironment(testBuilder = testBuilder)
        val deviceId = deviceSurvey.deviceId
        val now = Instant.now().atOffset( ZoneOffset.UTC )
        val currentHour = now.hour
        val currentDayOfWeek = now.dayOfWeek.value - 1

        // Answer first single select question with option 1 ten times and option 2 five times

        (1..10).forEach { _ ->
            createSingleSelectAnswer(
                testBuilder = testBuilder,
                deviceSurvey = deviceSurvey,
                page = pages[2],
                optionIndex = 0
            )
        }

        (1..5).forEach { _ ->
            createSingleSelectAnswer(
                testBuilder = testBuilder,
                deviceSurvey = deviceSurvey,
                page = pages[2],
                optionIndex = 1
            )
        }

        // Answer first multi select option with options 1 for five times, 1 and 2 for five times

        (1..5).forEach { _ ->
            createMultiSelectAnswer(
                testBuilder = testBuilder,
                deviceSurvey = deviceSurvey,
                page = pages[4],
                optionIndices = arrayOf(0)
            )
        }

        (1..5).forEach { _ ->
            createMultiSelectAnswer(
                testBuilder = testBuilder,
                deviceSurvey = deviceSurvey,
                page = pages[4],
                optionIndices = arrayOf(0, 1)
            )
        }

        // Ensure that statistics are updated

        val statistics = testBuilder.manager.deviceSurveys.getDeviceSurveyStatistics(
            deviceId = deviceId,
            surveyId = deviceSurvey.surveyId
        )

        assertEquals(25, statistics.totalAnswerCount)

        statistics.averages.hourly.forEachIndexed { hourIndex, hourAverage ->
            if (hourIndex == currentHour) {
                assertEquals(100.0, hourAverage)
            } else {
                assertEquals(0.0, hourAverage)
            }
        }

        statistics.averages.weekDays.forEachIndexed { weekDayIndex, weekDayAverage ->
            if (weekDayIndex == currentDayOfWeek) {
                assertEquals(100.0, weekDayAverage)
            } else {
                assertEquals(0.0, weekDayAverage)
            }
        }

        assertEquals(4, statistics.questions.size)

        statistics.questions.forEachIndexed { questionIndex, questionStatistics ->
            run {
                val page = pages[questionIndex + 2]
                assertEquals(questionStatistics.pageId, page.id)
                assertEquals(questionStatistics.questionType, page.question?.type)
                assertEquals(page.question?.options?.size, questionStatistics.options.size)

                questionStatistics.options.forEachIndexed { optionIndex, optionStatistics ->
                    when (questionIndex) {
                        0 -> {
                            when (optionIndex) {
                                0 -> {
                                    assertEquals(10, optionStatistics.answerCount)
                                }

                                1 -> {
                                    assertEquals(5, optionStatistics.answerCount)
                                }

                                else -> {
                                    assertEquals(0, optionStatistics.answerCount)
                                }
                            }
                        }
                        2 -> {
                            when (optionIndex) {
                                0 -> {
                                    assertEquals(10, optionStatistics.answerCount)
                                }

                                1 -> {
                                    assertEquals(5, optionStatistics.answerCount)
                                }

                                else -> {
                                    assertEquals(0, optionStatistics.answerCount)
                                }
                            }
                        }
                        else -> {
                            assertEquals(0, optionStatistics.answerCount)
                        }
                    }

                    assertEquals(page.question?.options?.get(optionIndex)?.questionOptionValue, optionStatistics.questionOptionValue)
                }
            }
        }
    }

    @Test
    fun testDeviceSurveyStatisticsEmpty() = createTestBuilder().use { testBuilder ->
        val ( deviceSurvey, pages ) = setupStatisticsEnvironment(testBuilder = testBuilder)
        val deviceId = deviceSurvey.deviceId
        val surveyId = deviceSurvey.surveyId

        // Assert that empty statistics are returned if no answers are given

        val emptyStatistics = testBuilder.manager.deviceSurveys.getDeviceSurveyStatistics(
            deviceId = deviceId,
            surveyId = deviceSurvey.surveyId
        )

        assertNotNull(emptyStatistics)

        assertEquals(0, emptyStatistics.totalAnswerCount)

        assertEquals(surveyId, emptyStatistics.surveyId)
        assertEquals(deviceId, emptyStatistics.deviceId)

        assertEquals(24, emptyStatistics.averages.hourly.size)
        assertEquals(7, emptyStatistics.averages.weekDays.size)
        emptyStatistics.averages.hourly.forEach { assertEquals(0.0, it) }
        emptyStatistics.averages.weekDays.forEach { assertEquals(0.0, it) }

        assertEquals(4, emptyStatistics.questions.size)

        emptyStatistics.questions.forEachIndexed { questionIndex, questionStatistics ->
            run {
                val page = pages[questionIndex + 2]
                assertEquals(questionStatistics.pageId, page.id)
                assertEquals(questionStatistics.questionType, page.question?.type)
                assertEquals(page.question?.options?.size, questionStatistics.options.size)

                questionStatistics.options.forEachIndexed{ optionIndex, optionStatistics ->
                    assertEquals(0, optionStatistics.answerCount)
                    assertEquals(page.question?.options?.get(optionIndex)?.questionOptionValue, optionStatistics.questionOptionValue)
                }
            }
        }
    }

    @Test
    fun testDeviceSurveyStatisticsPermissions() = createTestBuilder().use { testBuilder ->
        val ( deviceSurvey ) = setupStatisticsEnvironment(testBuilder = testBuilder)
        val deviceId = deviceSurvey.deviceId

        testBuilder.consumer.deviceSurveys.assertGetDeviceSurveyStatisticsFail(
            expectedStatusCode = 403,
            deviceId = deviceId,
            surveyId = deviceSurvey.surveyId
        )

        testBuilder.empty.deviceSurveys.assertGetDeviceSurveyStatisticsFail(
            expectedStatusCode = 401,
            deviceId = deviceId,
            surveyId = deviceSurvey.surveyId
        )

        testBuilder.notvalid.deviceSurveys.assertGetDeviceSurveyStatisticsFail(
            expectedStatusCode = 401,
            deviceId = deviceId,
            surveyId = deviceSurvey.surveyId
        )
    }

    /**
     * Setup environment for testing statistics
     *
     * @param testBuilder TestBuilder
     * @return created device survey and pages
     */
    private fun setupStatisticsEnvironment(testBuilder: TestBuilder): Pair<DeviceSurvey, List<Page>> {
        val ( deviceId, deviceKey ) = testBuilder.manager.devices.setupTestDevice()

        val survey = testBuilder.manager.surveys.createDefault()
        val surveyId = survey.id!!

        approveSurvey(survey)

        val deviceSurvey = testBuilder.manager.deviceSurveys.create(
            deviceId = deviceId,
            DeviceSurvey(
                surveyId = surveyId,
                deviceId = deviceId,
                status = DeviceSurveyStatus.PUBLISHED,
                publishStartTime = OffsetDateTime.now().toString(),
                publishEndTime = OffsetDateTime.now().plusDays(1).toString()
            )
        )

        val layout = testBuilder.manager.layouts.createDefault()

        // Page 1 and 2 are not a questions
        // Page 3 and 4 are a single choice questions
        // Page 5 and 6 are a multiple choice questions

        val pages = (1..6).mapNotNull {
            val question = if (it <= 2)
                null
            else if (it <= 4)
                PageQuestion(
                    type = PageQuestionType.SINGLE_SELECT,
                    options = arrayOf(
                        PageQuestionOption(
                            questionOptionValue = "1",
                            orderNumber = 0
                        ),
                        PageQuestionOption(
                            questionOptionValue = "2",
                            orderNumber = 1
                        ),
                        PageQuestionOption(
                            questionOptionValue = "3",
                            orderNumber = 2
                        )
                    )
                )
            else {
                PageQuestion(
                    type = PageQuestionType.MULTI_SELECT,
                    options = arrayOf(
                        PageQuestionOption(
                            questionOptionValue = "1",
                            orderNumber = 0
                        ),
                        PageQuestionOption(
                            questionOptionValue = "2",
                            orderNumber = 1
                        ),
                        PageQuestionOption(
                            questionOptionValue = "3",
                            orderNumber = 2
                        )
                    )
                )
            }

            testBuilder.manager.pages.create(
                surveyId = surveyId,
                page = Page(
                    orderNumber = it,
                    title = "title of page $it",
                    question = question,
                    layoutId = layout.id!!,
                    nextButtonVisible = true
                )
            )
        }

        testBuilder.manager.deviceData.setDeviceKey(deviceKey)

        return Pair(deviceSurvey, pages)
    }
}