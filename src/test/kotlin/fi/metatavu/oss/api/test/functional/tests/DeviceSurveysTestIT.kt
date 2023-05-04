package fi.metatavu.oss.api.test.functional.tests

import fi.metatavu.oss.api.test.functional.resources.LocalTestProfile
import fi.metatavu.oss.test.client.models.DeviceSurvey
import fi.metatavu.oss.test.client.models.DeviceSurveyStatus
import fi.metatavu.oss.test.client.models.Survey
import fi.metatavu.oss.test.client.models.SurveyStatus
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.*

/**
 * Tests for device surveys API
 */
@QuarkusTest
@TestProfile(LocalTestProfile::class)
class DeviceSurveysTestIT: AbstractResourceTest() {

    @Test
    fun testCreateDeviceSurvey() {
        createTestBuilder().use { testBuilder ->
            val (deviceId) = testBuilder.manager.deviceSurveys.setupTestDevice()
            val createdSurvey = testBuilder.manager.surveys.createDefault()
            testBuilder.manager.surveys.update(
                surveyId = createdSurvey.id!!,
                newSurvey = createdSurvey.copy(status = SurveyStatus.APPROVED)
            )
            val deviceSurveyToCreate = DeviceSurvey(
                surveyId = createdSurvey.id,
                deviceId = deviceId,
                status = DeviceSurveyStatus.PUBLISHED,
            )
            val createdDeviceSurvey = testBuilder.manager.deviceSurveys.create(
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

            // permissions
            testBuilder.consumer.deviceSurveys.assertCreateFail(403, deviceId, deviceSurveyToCreate)
            testBuilder.empty.deviceSurveys.assertCreateFail(401, deviceId, deviceSurveyToCreate)
            testBuilder.notvalid.deviceSurveys.assertCreateFail(401, deviceId, deviceSurveyToCreate)
        }
    }

    @Test
    fun testListDeviceSurveys() {
        createTestBuilder().use { testBuilder ->
            val (deviceId) = testBuilder.manager.deviceSurveys.setupTestDevice()
            val (deviceId2) = testBuilder.manager.deviceSurveys.setupTestDevice("321")
            val createdSurveys = mutableListOf<Survey>()

            val createdSurveyDevice2 = testBuilder.manager.surveys.create(
                survey = Survey(
                    title = "device-2-test-survey",
                    status = SurveyStatus.DRAFT,
                    timeout = 60
                )
            )
            testBuilder.manager.surveys.update(
                surveyId = createdSurveyDevice2.id!!,
                newSurvey = createdSurveyDevice2.copy(status = SurveyStatus.APPROVED)
            )
            testBuilder.manager.deviceSurveys.create(
                deviceId = deviceId2,
                deviceSurvey = DeviceSurvey(
                    surveyId = createdSurveyDevice2.id,
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
                        status = DeviceSurveyStatus.PUBLISHED
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
    }

    @Test
    fun testFindDeviceSurvey() {
        createTestBuilder().use { testBuilder ->
            val (deviceId, deviceKey) = testBuilder.manager.deviceSurveys.setupTestDevice()
            testBuilder.manager.deviceSurveys.setDeviceKey(deviceKey)
            val createdSurvey = testBuilder.manager.surveys.createDefault()
            val deviceSurveyToCreate = DeviceSurvey(
                surveyId = createdSurvey.id!!,
                deviceId = deviceId,
                status = DeviceSurveyStatus.PUBLISHED
            )
            testBuilder.manager.surveys.update(
                surveyId = createdSurvey.id,
                newSurvey = createdSurvey.copy(status = SurveyStatus.APPROVED)
            )
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
    }

    @Test
    fun testUpdateDeviceSurvey() {
        createTestBuilder().use { testBuilder ->
            val (deviceId) = testBuilder.manager.deviceSurveys.setupTestDevice()
            val createdSurvey = testBuilder.manager.surveys.createDefault()
            testBuilder.manager.surveys.update(
                surveyId = createdSurvey.id!!,
                newSurvey = createdSurvey.copy(status = SurveyStatus.APPROVED)
            )
            val createdDeviceSurvey = testBuilder.manager.deviceSurveys.create(
                deviceId = deviceId,
                deviceSurvey = DeviceSurvey(
                    surveyId = createdSurvey.id,
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
        }
    }

    @Test
    fun testUpdateDeviceSurveyFail() {
        createTestBuilder().use { testBuilder ->
            val (deviceId) = testBuilder.manager.deviceSurveys.setupTestDevice()
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
    }

    @Test
    fun testInvalidScheduledDeviceSurvey() {
        createTestBuilder().use { testBuilder ->
            val (deviceId) = testBuilder.manager.deviceSurveys.setupTestDevice()
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
    }

    @Test
    fun testDeleteDeviceSurvey() {
        createTestBuilder().use { testBuilder ->
            val (deviceId, deviceKey) = testBuilder.manager.deviceSurveys.setupTestDevice()
            testBuilder.manager.deviceSurveys.setDeviceKey(deviceKey)
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
    }

    @Test
    fun testCreateInvalidDeviceSurvey() {
        createTestBuilder().use { testBuilder ->
            val (deviceId) = testBuilder.manager.deviceSurveys.setupTestDevice()
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
    }

    @Test
    fun testDeleteInvalidDeviceSurvey() {
        createTestBuilder().use { testBuilder ->
            val (deviceId) = testBuilder.manager.deviceSurveys.setupTestDevice()
            // Device doesn't exist
            testBuilder.manager.deviceSurveys.assertDeleteFail(
                expectedStatusCode = 404,
                deviceId = UUID.randomUUID(),
                deviceSurveyId = UUID.randomUUID()
            )
            // Survey doesn't exist
            testBuilder.manager.deviceSurveys.assertDeleteFail(
                expectedStatusCode = 400,
                deviceId = deviceId,
                deviceSurveyId = UUID.randomUUID()
            )
        }
    }

    @Test
    fun testFindDeviceSurveyFail() {
        createTestBuilder().use { testBuilder ->
            val (deviceId1, deviceKey1) = testBuilder.manager.deviceSurveys.setupTestDevice()
            val (deviceId2, deviceKey2) = testBuilder.manager.deviceSurveys.setupTestDevice("321")

            // Device 1 surveys with device 2 key
            testBuilder.manager.deviceSurveys.setDeviceKey(deviceKey2)
            testBuilder.manager.deviceSurveys.assertFindFail(
                expectedStatusCode = 401,
                deviceId = deviceId1,
                deviceSurveyId = UUID.randomUUID()
            )

            // Device 2 surveys with device 1 key
            testBuilder.manager.deviceSurveys.setDeviceKey(deviceKey1)
            testBuilder.manager.deviceSurveys.assertFindFail(
                expectedStatusCode = 401,
                deviceId = deviceId2,
                deviceSurveyId = UUID.randomUUID()
            )

            // Device not found
            testBuilder.manager.deviceSurveys.setDeviceKey(deviceKey1)
            testBuilder.manager.deviceSurveys.assertFindFail(
                expectedStatusCode = 404,
                deviceId = deviceId1,
                deviceSurveyId = UUID.randomUUID()
            )
            // Device Survey not found
            testBuilder.manager.deviceSurveys.setDeviceKey(deviceKey1)
            testBuilder.manager.deviceSurveys.assertFindFail(
                expectedStatusCode = 404,
                deviceId = deviceId1,
                deviceSurveyId = UUID.randomUUID()
            )
        }
    }
}