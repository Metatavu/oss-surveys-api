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
 * Tests for device survey data API
 */
@QuarkusTest
@TestProfile(LocalTestProfile::class)
class DeviceSurveyDataTestIT : AbstractResourceTest() {

    @Test
    fun testListDeviceSurveys() {
        createTestBuilder().use { tb ->
            val (device1Id, device1Key) = tb.manager.devices.setupTestDevice(serialNumber = "1")
            val (device2Id, _) = tb.manager.devices.setupTestDevice(serialNumber = "2")

            val createdSurvey1 = tb.manager.surveys.createDefault()
            val createdSurvey2 = tb.manager.surveys.createDefault()
            approveSurvey(createdSurvey1)
            approveSurvey(createdSurvey2)

            tb.manager.deviceSurveys.create(
                deviceId = device1Id,
                deviceSurvey = DeviceSurvey(
                    surveyId = createdSurvey1.id!!,
                    deviceId = device1Id,
                    status = DeviceSurveyStatus.PUBLISHED,
                    publishStartTime = OffsetDateTime.now().toString(),
                    publishEndTime = OffsetDateTime.now().plusDays(1).toString()
                )
            )
            tb.manager.deviceSurveys.create(
                deviceId = device2Id,
                deviceSurvey = DeviceSurvey(
                    surveyId = createdSurvey2.id!!,
                    deviceId = device2Id,
                    status = DeviceSurveyStatus.SCHEDULED,
                    publishStartTime = OffsetDateTime.now().plusDays(1).toString(),
                    publishEndTime = OffsetDateTime.now().plusDays(2).toString()
                )
            )

            tb.manager.deviceData.setDeviceKey(device1Key)
            val foundDeviceSurveysForDevice1Published = tb.manager.deviceData.list(
                deviceId = device1Id
            )
            assertEquals(1, foundDeviceSurveysForDevice1Published.size)
            assertEquals(createdSurvey1.id, foundDeviceSurveysForDevice1Published[0].surveyId)

            // cannot list for another device's key
            tb.manager.deviceData.assertListFail(
                deviceId = device2Id,
                expectedStatusCode = 401
            )

        }
    }

    @Test
    fun testFindDeviceSurveyData() {
        createTestBuilder().use { tb ->
            val (deviceId, deviceKey) = tb.manager.devices.setupTestDevice()
            val createdSurvey = tb.manager.surveys.createDefault()
            val createdLayout = tb.manager.layouts.createDefault()
            val createdPages = (1..3).map { i ->
                tb.manager.pages.create(
                    surveyId = createdSurvey.id!!,
                    page = Page(
                        orderNumber = i,
                        layoutId = createdLayout.id!!,
                        title = "Page $i",
                        properties = arrayOf(
                            PageProperty(
                                key = "question",
                                value = "question $i",
                                type = PagePropertyType.TEXT
                            ),
                            PageProperty(
                                key = "answers",
                                value = "[\"answerOption1\", \"answerOption2\"]",
                                PagePropertyType.OPTIONS
                            )
                        )
                    )
                )
            }
            approveSurvey(createdSurvey)

            val now = OffsetDateTime.now()
            val createdDeviceSurvey = tb.manager.deviceSurveys.create(
                deviceId = deviceId,
                deviceSurvey = DeviceSurvey(
                    surveyId = createdSurvey.id!!,
                    deviceId = deviceId,
                    status = DeviceSurveyStatus.PUBLISHED,
                    publishStartTime = now.toString(),
                    publishEndTime = now.plusDays(1).toString()
                )
            )

            tb.manager.deviceData.setDeviceKey(deviceKey)
            val foundSurveyData = tb.manager.deviceData.find(
                deviceId = deviceId,
                deviceSurveyId = createdDeviceSurvey.id!!
            )

            assertEquals(createdDeviceSurvey.id, foundSurveyData.id)
            assertEquals(createdDeviceSurvey.deviceId, foundSurveyData.deviceId)
            assertEquals(createdDeviceSurvey.surveyId, foundSurveyData.surveyId)
            assertEquals(createdDeviceSurvey.status, foundSurveyData.status)
            assertEquals(OffsetDateTime.parse(createdDeviceSurvey.publishStartTime).toEpochSecond(), OffsetDateTime.parse(foundSurveyData.publishStartTime).toEpochSecond())
            assertEquals(OffsetDateTime.parse(createdDeviceSurvey.publishEndTime).toEpochSecond(), OffsetDateTime.parse(foundSurveyData.publishEndTime).toEpochSecond())
            assertEquals(createdSurvey.title, foundSurveyData.title)
            assertEquals(createdSurvey.timeout, foundSurveyData.timeout)

            assertEquals(3, foundSurveyData.pages?.size)
            val page1Data = foundSurveyData.pages?.get(0)
            assertNotNull(page1Data)
            assertEquals(createdPages[0]!!.id, page1Data!!.id)
            assertEquals(createdLayout.html, page1Data.layoutHtml)

            assertEquals(2, page1Data.properties?.size)
            val propertyData1 = page1Data.properties?.find { it.key == "question" }
            val propertyData2 = page1Data.properties?.find { it.key == "answers" }
            assertEquals(PagePropertyType.TEXT, propertyData1?.type)
            assertEquals("question 1", propertyData1?.value)
            assertEquals(PagePropertyType.OPTIONS, propertyData2?.type)
            assertEquals("[\"answerOption1\", \"answerOption2\"]", propertyData2?.value)

            tb.manager.deviceData.assertFindFail(401, UUID.randomUUID(), createdDeviceSurvey.id)
            tb.manager.deviceData.assertFindFail(404, deviceId, UUID.randomUUID())

            // Check that only correct Device's ID allows finding the survey
            val (_, deviceKey2) = tb.manager.devices.setupTestDevice(serialNumber = "124")
            tb.manager.deviceData.setDeviceKey(deviceKey2)
            tb.manager.deviceData.assertFindFail(401, deviceId, createdDeviceSurvey.id)
        }
    }
}