package fi.metatavu.oss.api.test.functional.tests

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.metatavu.oss.api.test.functional.TestBuilder
import fi.metatavu.oss.api.test.functional.resources.LocalTestProfile
import fi.metatavu.oss.test.client.models.*
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import org.junit.jupiter.api.Assertions.*
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
            val createdLayout = tb.manager.layouts.create(
                Layout(
                    name = "layout",
                    thumbnail = "thumbnail",
                    html = "html",
                    layoutVariables = arrayOf(
                        LayoutVariable(
                            type = LayoutVariableType.TEXT,
                            key = "key"
                        )
                    )
                )
            )
            val createdPages = (1..3).map { i ->
                tb.manager.pages.create(
                    surveyId = createdSurvey.id!!,
                    page = Page(
                        orderNumber = i,
                        layoutId = createdLayout.id!!,
                        title = "Page $i",
                        nextButtonVisible = true,
                        properties = arrayOf(
                            PageProperty(
                                key = "htmlVariable",
                                value = "value $i"
                            )
                        ),
                        question = PageQuestion(
                            type = PageQuestionType.SINGLE_SELECT,
                            options = arrayOf(
                                PageQuestionOption(
                                    orderNumber = 0,
                                    questionOptionValue = "Option 1"
                                )
                            )
                        )
                    )
                )
            }
            approveSurvey(createdSurvey)

            val createdDeviceSurvey = tb.manager.deviceSurveys.createCurrentlyPublishedDeviceSurvey(
                deviceId = deviceId,
                surveyId = createdSurvey.id!!
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

            // verify page properties
            assertEquals(1, page1Data.properties?.size)
            val propertyData1 = page1Data.properties?.find { it.key == "htmlVariable" }
            assertEquals("value 1", propertyData1?.value)

            //verify page layout
            assertEquals(createdLayout.html, page1Data.layoutHtml)
            assertEquals(1, page1Data.layoutVariables?.size)
            assertEquals(createdLayout.layoutVariables!![0].key, page1Data.layoutVariables!![0].key)
            assertEquals(createdLayout.layoutVariables[0].type, page1Data.layoutVariables[0].type)

            //verify page question
            val page1Question = page1Data.question
            assertEquals(PageQuestionType.SINGLE_SELECT, page1Question?.type)
            assertEquals(1, page1Question?.options?.size)
            assertEquals("Option 1", page1Question?.options?.get(0)?.questionOptionValue)

            tb.manager.deviceData.assertFindFail(401, UUID.randomUUID(), createdDeviceSurvey.id)
            tb.manager.deviceData.assertFindFail(404, deviceId, UUID.randomUUID())

            // Check that only correct Device's ID allows finding the survey
            val (_, deviceKey2) = tb.manager.devices.setupTestDevice(serialNumber = "124")
            tb.manager.deviceData.setDeviceKey(deviceKey2)
            tb.manager.deviceData.assertFindFail(401, deviceId, createdDeviceSurvey.id)
        }
    }

    /**
     * Tests submitting 2 text answers to the page, listing and finding those
     */
    @Test
    fun submitTextAnswer() {
        createTestBuilder().use { tb ->
            val (deviceId, deviceKey) = tb.manager.devices.setupTestDevice()
            tb.manager.deviceData.setDeviceKey(deviceKey)
            val createdSurvey = tb.manager.surveys.createDefault()
            val layout = tb.manager.layouts.createDefault()
            approveSurvey(createdSurvey)
            val deviceSurvey = tb.manager.deviceSurveys.createCurrentlyPublishedDeviceSurvey(
                deviceId = deviceId,
                surveyId = createdSurvey.id!!
            )

            //Create one page with freetext answer
            val page = tb.manager.pages.create(
                surveyId = createdSurvey.id,
                page = Page(
                    orderNumber = 0,
                    title = "best beer in the universe?",
                    question = PageQuestion(
                        type = PageQuestionType.FREETEXT,
                        options = emptyArray()
                    ),
                    layoutId = layout.id!!,
                    nextButtonVisible = true
                )
            )

            // submit answer 1
            createAnswer(
                tb = tb,
                deviceSurvey = deviceSurvey,
                page = page!!,
                answer = "Karhu"
            )

            //another answer 2
            createAnswer(
                tb = tb,
                deviceSurvey = deviceSurvey,
                page = page,
                answer = "Guinness"
            )

            // test listing
            val answers = tb.manager.surveyAnswers.list(
                surveyId = createdSurvey.id,
                pageId = page.id!!
            )

            assertEquals(2, answers.size)
            val firstAnswer = answers.find { it.answer == "Karhu" }
            assertNotNull(firstAnswer?.id)
            assertEquals(page.id, firstAnswer?.pageId)
            assertNotNull(firstAnswer?.metadata?.createdAt)
            assertNotNull(firstAnswer?.metadata?.modifiedAt)
            // anonymous answers do not have creator id
            assertNull(firstAnswer?.metadata?.creatorId)

            // test finding by id
            val firstAnswerFound = tb.manager.surveyAnswers.find(
                surveyId = createdSurvey.id,
                pageId = page.id,
                answerId = firstAnswer!!.id!!
            )

            assertNotNull(firstAnswerFound.id)
            assertEquals(firstAnswer.pageId, firstAnswerFound.pageId)
            assertEquals(firstAnswer.answer, firstAnswerFound.answer)
        }
    }

    /**
     * Tests submitting 2 single select answers to the page and listing those
     */
    @Test
    fun submitSingleSelectAnswer() {
        createTestBuilder().use { tb ->
            val (deviceId, deviceKey) = tb.manager.devices.setupTestDevice()
            tb.manager.deviceData.setDeviceKey(deviceKey)
            val createdSurvey = tb.manager.surveys.createDefault()
            val layout = tb.manager.layouts.createDefault()
            approveSurvey(createdSurvey)
            val deviceSurvey = tb.manager.deviceSurveys.createCurrentlyPublishedDeviceSurvey(
                deviceId = deviceId,
                surveyId = createdSurvey.id!!
            )

            val singleOptions = (0..1).map {
                PageQuestionOption(
                    orderNumber = it,
                    questionOptionValue = "Option $it"
                )
            }.toTypedArray()

            //Create one page with signle select question
            val page = tb.manager.pages.create(
                surveyId = createdSurvey.id,
                page = Page(
                    orderNumber = 0,
                    title = "Select one option",
                    question = PageQuestion(
                        type = PageQuestionType.SINGLE_SELECT,
                        options = singleOptions
                    ),
                    layoutId = layout.id!!,
                    nextButtonVisible = true
                )
            )
            // Get IDs of the options to fill in the answers
            val answerOption0 = page!!.question!!.options.find { it.orderNumber == 0 }!!
            val answerOption1 = page.question!!.options.find { it.orderNumber == 1 }!!

            // Submit one answer with first option
            createAnswer(
                tb = tb,
                deviceSurvey = deviceSurvey,
                page = page,
                answer = answerOption0.id.toString()
            )

            // submit another answer with second option
            createAnswer(
                tb = tb,
                deviceSurvey = deviceSurvey,
                page = page,
                answer = answerOption1.id.toString()
            )

            val answers = tb.manager.surveyAnswers.list(
                surveyId = createdSurvey.id,
                pageId = page.id!!
            )

            assertEquals(2, answers.size)
            val answer0 = answers.find { it.answer == answerOption0.id.toString() }
            assertEquals(page.id, answer0?.pageId)
            assertNotNull(answer0?.id)

            val answer1 = answers.find { it.answer == answerOption0.id.toString() }
            assertEquals(page.id, answer1?.pageId)
            assertNotNull(answer1?.id)

            // Tests for errors
            tb.manager.deviceData.assertCreateFail(
                deviceId = deviceId,
                deviceSurveyId = deviceSurvey.id!!,
                pageId = page.id!!,
                devicePageSurveyAnswer = DevicePageSurveyAnswer(
                    pageId = page.id,
                    answer = "qwerty"  // invalid uuid
                ),
                expectedStatusCode = 400
            )
        }
    }

    /**
     * Tests submitting 2 multi select answers and verifying those
     */
    @Test
    fun testMutliSelectAnswers() {
        createTestBuilder().use { tb ->
            val (deviceId, deviceKey) = tb.manager.devices.setupTestDevice()
            tb.manager.deviceData.setDeviceKey(deviceKey)
            val createdSurvey = tb.manager.surveys.createDefault()
            val layout = tb.manager.layouts.createDefault()
            approveSurvey(createdSurvey)
            val deviceSurvey = tb.manager.deviceSurveys.createCurrentlyPublishedDeviceSurvey(
                deviceId = deviceId,
                surveyId = createdSurvey.id!!
            )

            val multiOptions = (0..2).map {
                PageQuestionOption(
                    orderNumber = it,
                    questionOptionValue = "Option $it"
                )
            }.toTypedArray()

            //Create one page with signle select question
            val page = tb.manager.pages.create(
                surveyId = createdSurvey.id,
                page = Page(
                    orderNumber = 0,
                    title = "what is your name",
                    question = PageQuestion(
                        type = PageQuestionType.MULTI_SELECT,
                        options = multiOptions
                    ),
                    layoutId = layout.id!!,
                    nextButtonVisible = true
                )
            )
            val answerOption0 = page!!.question!!.options.find { it.orderNumber == multiOptions[0].orderNumber }!!
            val answerOption1 = page.question!!.options.find { it.orderNumber == multiOptions[1].orderNumber }!!
            val answerOption2 = page.question.options.find { it.orderNumber == multiOptions[2].orderNumber }!!

            val asnwerString0 = jacksonObjectMapper().writeValueAsString(
                listOf(
                    answerOption0.id,
                    answerOption1.id
                )
            )
            createAnswer(
                tb = tb,
                deviceSurvey = deviceSurvey,
                page = page,
                answer = asnwerString0
            )

            val asnwerString1 = jacksonObjectMapper().writeValueAsString(
                listOf(
                    answerOption0.id,
                    answerOption2.id
                )
            )
            createAnswer(
                tb = tb,
                deviceSurvey = deviceSurvey,
                page = page,
                answer = asnwerString1
            )

            val answers = tb.manager.surveyAnswers.list(
                surveyId = createdSurvey.id,
                pageId = page.id!!
            )
            assertEquals(2, answers.size)
            val answer0 = answers.find { it.answer!!.contains(answerOption0.id.toString()) &&  it.answer.contains(answerOption1.id.toString()) }
            assertEquals(page.id, answer0?.pageId)
            assertNotNull(answer0?.id)

            val answer1 = answers.find { it.answer!!.contains(answerOption0.id.toString()) &&  it.answer.contains(answerOption2.id.toString()) }
            assertEquals(page.id, answer1?.pageId)
            assertNotNull(answer1?.id)

            // Tests for errors
            tb.manager.deviceData.assertCreateFail(
                deviceId = deviceId,
                deviceSurveyId = deviceSurvey.id!!,
                pageId = page.id,
                devicePageSurveyAnswer = DevicePageSurveyAnswer(
                    pageId = page.id,
                    answer = "${answerOption0.id} ${answerOption1.id}"  // invalid json array
                ),
                expectedStatusCode = 400
            )
        }
    }


    @Test
    fun submitInvalidAnswers() {
        createTestBuilder().use { tb ->
            val (deviceId, deviceKey) = tb.manager.devices.setupTestDevice()
            tb.manager.deviceData.setDeviceKey(deviceKey)
            val createdSurvey = tb.manager.surveys.createDefault()
            val createdSurvey2 = tb.manager.surveys.createDefault()
            val layout = tb.manager.layouts.createDefault()
            approveSurvey(createdSurvey)
            approveSurvey(createdSurvey2)

            val deviceSurvey1 = tb.manager.deviceSurveys.createCurrentlyPublishedDeviceSurvey(
                deviceId = deviceId,
                surveyId = createdSurvey.id!!
            )
            val deviceSurvey2 = tb.manager.deviceSurveys.createCurrentlyPublishedDeviceSurvey(
                deviceId = deviceId,
                surveyId = createdSurvey2.id!!
            )
            //Create one page with freetext answer
            val page = tb.manager.pages.create(
                surveyId = createdSurvey.id,
                page = Page(
                    orderNumber = 0,
                    title = "best beer in the universe?",
                    question = PageQuestion(
                        type = PageQuestionType.FREETEXT,
                        options = emptyArray()
                    ),
                    layoutId = layout.id!!,
                    nextButtonVisible = true
                )
            )

            // Tests for errors
            val randomUUID = UUID.randomUUID()
            tb.manager.deviceData.assertCreateFail(
                deviceId = randomUUID,
                deviceSurveyId = deviceSurvey1.id!!,
                pageId = page!!.id!!,
                devicePageSurveyAnswer = DevicePageSurveyAnswer(
                    pageId = page.id,
                    answer = "qwerty"
                ),
                expectedStatusCode = 401
            )

            tb.manager.deviceData.assertCreateFail(
                deviceId = deviceId,
                deviceSurveyId = deviceSurvey1.id!!,
                pageId = randomUUID,
                devicePageSurveyAnswer = DevicePageSurveyAnswer(
                    pageId = page.id,
                    answer = "qwerty"
                ),
                expectedStatusCode = 404
            )
            tb.manager.deviceData.assertCreateFail(
                deviceId = deviceId,
                deviceSurveyId = randomUUID,
                pageId = page.id!!,
                devicePageSurveyAnswer = DevicePageSurveyAnswer(
                    pageId = page.id,
                    answer = "qwerty"
                ),
                expectedStatusCode = 404
            )
            tb.manager.deviceData.assertCreateFail(
                deviceId = deviceId,
                deviceSurveyId = deviceSurvey1.id,
                pageId = page.id,
                devicePageSurveyAnswer = DevicePageSurveyAnswer(
                    pageId = randomUUID,
                    answer = "qwerty"
                ),
                expectedStatusCode = 404
            )

            // cannot asnwer for survey that the page does not belong to
            tb.manager.deviceData.assertCreateFail(
                deviceId = deviceId,
                deviceSurveyId = deviceSurvey2.id!!,
                pageId = page.id!!,
                devicePageSurveyAnswer = DevicePageSurveyAnswer(
                    pageId = page.id,
                    answer = "qwerty"
                ),
                expectedStatusCode = 404
            )

            tb.manager.deviceData.setDeviceKey("invalid key")
            tb.manager.deviceData.assertCreateFail(
                deviceId = deviceId,
                deviceSurveyId = deviceSurvey1.id!!,
                pageId = page.id,
                devicePageSurveyAnswer = DevicePageSurveyAnswer(
                    pageId = page.id,
                    answer = "qwerty"
                ),
                expectedStatusCode = 403
            )
        }
    }

    /**
     * Creates an answer for a page
     *
     * @param tb test builder
     * @param deviceSurvey device survey
     * @param page page
     * @param answer answer
     */
    private fun createAnswer(
        tb: TestBuilder,
        deviceSurvey: DeviceSurvey,
        page: Page,
        answer: String
    ) {
        tb.manager.deviceData.submitSurveyAnswer(
            deviceId = deviceSurvey.deviceId,
            deviceSurveyId = deviceSurvey.id!!,
            pageId = page.id!!,
            devicePageSurveyAnswer = DevicePageSurveyAnswer(
                pageId = page.id,
                answer = answer
            ),
            surveyId = deviceSurvey.surveyId
        )
    }

}