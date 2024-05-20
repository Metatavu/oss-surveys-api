package fi.metatavu.oss.api.test.functional.tests

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.metatavu.oss.api.impl.pages.questions.PageQuestionController
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
    fun testListDeviceSurveys() = createTestBuilder().use { testBuilder ->
        val (device1Id, device1Key) = testBuilder.manager.devices.setupTestDevice(serialNumber = "1")
        val (device2Id, _) = testBuilder.manager.devices.setupTestDevice(serialNumber = "2")

        val createdSurvey1 = testBuilder.manager.surveys.createDefault()
        val createdSurvey2 = testBuilder.manager.surveys.createDefault()
        approveSurvey(createdSurvey1)
        approveSurvey(createdSurvey2)

        testBuilder.manager.deviceSurveys.create(
            deviceId = device1Id,
            deviceSurvey = DeviceSurvey(
                surveyId = createdSurvey1.id!!,
                deviceId = device1Id,
                status = DeviceSurveyStatus.PUBLISHED,
                publishStartTime = OffsetDateTime.now().toString(),
                publishEndTime = OffsetDateTime.now().plusDays(1).toString()
            )
        )
        testBuilder.manager.deviceSurveys.create(
            deviceId = device2Id,
            deviceSurvey = DeviceSurvey(
                surveyId = createdSurvey2.id!!,
                deviceId = device2Id,
                status = DeviceSurveyStatus.SCHEDULED,
                publishStartTime = OffsetDateTime.now().plusDays(1).toString(),
                publishEndTime = OffsetDateTime.now().plusDays(2).toString()
            )
        )

        testBuilder.manager.deviceData.setDeviceKey(device1Key)
        val foundDeviceSurveysForDevice1Published = testBuilder.manager.deviceData.list(
            deviceId = device1Id
        )
        assertEquals(1, foundDeviceSurveysForDevice1Published.size)
        assertEquals(createdSurvey1.id, foundDeviceSurveysForDevice1Published[0].surveyId)

        // cannot list for another device's key
        testBuilder.manager.deviceData.assertListFail(
            deviceId = device2Id,
            expectedStatusCode = 401
        )

    }

    @Test
    fun testFindDeviceSurveyData() = createTestBuilder().use { testBuilder ->
        val (deviceId, deviceKey) = testBuilder.manager.devices.setupTestDevice()
        val createdSurvey = testBuilder.manager.surveys.createDefault()
        val createdLayout = testBuilder.manager.layouts.create(
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
            testBuilder.manager.pages.create(
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

        val createdDeviceSurvey = testBuilder.manager.deviceSurveys.createCurrentlyPublishedDeviceSurvey(
            deviceId = deviceId,
            surveyId = createdSurvey.id!!
        )

        testBuilder.manager.deviceData.setDeviceKey(deviceKey)
        val foundSurveyData = testBuilder.manager.deviceData.find(
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
        assertEquals(createdPages[0].id, page1Data!!.id)

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

        testBuilder.manager.deviceData.assertFindFail(401, UUID.randomUUID(), createdDeviceSurvey.id)
        testBuilder.manager.deviceData.assertFindFail(404, deviceId, UUID.randomUUID())

        // Check that only correct Device's ID allows finding the survey
        val (_, deviceKey2) = testBuilder.manager.devices.setupTestDevice(serialNumber = "124")
        testBuilder.manager.deviceData.setDeviceKey(deviceKey2)
        testBuilder.manager.deviceData.assertFindFail(401, deviceId, createdDeviceSurvey.id)
    }

    /**
     * Tests submitting 2 text answers to the page, listing and finding those
     */
    @Test
    fun submitTextAnswer() = createTestBuilder().use { testBuilder ->
        val (deviceId, deviceKey) = testBuilder.manager.devices.setupTestDevice()
        testBuilder.manager.deviceData.setDeviceKey(deviceKey)
        val createdSurvey = testBuilder.manager.surveys.createDefault()
        val layout = testBuilder.manager.layouts.createDefault()
        approveSurvey(createdSurvey)
        val deviceSurvey = testBuilder.manager.deviceSurveys.createCurrentlyPublishedDeviceSurvey(
            deviceId = deviceId,
            surveyId = createdSurvey.id!!
        )

        //Create one page with freetext answer
        val page = testBuilder.manager.pages.create(
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
        createPageAnswer(
            testBuilder = testBuilder,
            deviceSurvey = deviceSurvey,
            page = page,
            answer = "Karhu"
        )

        //another answer 2
        createPageAnswer(
            testBuilder = testBuilder,
            deviceSurvey = deviceSurvey,
            page = page,
            answer = "Guinness"
        )

        // test listing
        val answers = testBuilder.manager.surveyAnswers.list(
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
        val firstAnswerFound = testBuilder.manager.surveyAnswers.find(
            surveyId = createdSurvey.id,
            pageId = page.id,
            answerId = firstAnswer!!.id!!
        )

        assertNotNull(firstAnswerFound.id)
        assertEquals(firstAnswer.pageId, firstAnswerFound.pageId)
        assertEquals(firstAnswer.answer, firstAnswerFound.answer)
    }

    /**
     * Tests submitting 2 single select answers to the page and listing those
     */
    @Test
    fun submitSingleSelectAnswer() = createTestBuilder().use { testBuilder ->
        val (deviceId, deviceKey) = testBuilder.manager.devices.setupTestDevice()
        testBuilder.manager.deviceData.setDeviceKey(deviceKey)
        val createdSurvey = testBuilder.manager.surveys.createDefault()
        val layout = testBuilder.manager.layouts.createDefault()
        approveSurvey(createdSurvey)
        val deviceSurvey = testBuilder.manager.deviceSurveys.createCurrentlyPublishedDeviceSurvey(
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
        val page = testBuilder.manager.pages.create(
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
        val answerOption0 = getOptionValueByOrderNumber(page = page, orderNumber = 0)
        val answerOption1 = getOptionValueByOrderNumber(page = page, orderNumber = 1)

        // Submit one answer with first option
        createPageAnswer(
            testBuilder = testBuilder,
            deviceSurvey = deviceSurvey,
            page = page,
            answer = answerOption0
        )

        // submit another answer with second option
        createPageAnswer(
            testBuilder = testBuilder,
            deviceSurvey = deviceSurvey,
            page = page,
            answer = answerOption1
        )

        val answers = testBuilder.manager.surveyAnswers.list(
            surveyId = createdSurvey.id,
            pageId = page.id!!
        )

        assertEquals(2, answers.size)
        val answer0 = answers.find { it.answer == answerOption0 }
        assertEquals(page.id, answer0?.pageId)
        assertNotNull(answer0?.id)

        val answer1 = answers.find { it.answer == answerOption0 }
        assertEquals(page.id, answer1?.pageId)
        assertNotNull(answer1?.id)

        // Tests for errors
        testBuilder.manager.deviceData.assertCreateFail(
            deviceId = deviceId,
            deviceSurveyId = deviceSurvey.id!!,
            pageId = page.id,
            devicePageSurveyAnswer = DevicePageSurveyAnswer(
                pageId = page.id,
                answer = "qwerty"  // invalid uuid
            ),
            expectedStatusCode = 400
        )
    }

    /**
     * Tests submitting 2 multi select answers and verifying those
     */
    @Test
    fun testMultiSelectAnswers() = createTestBuilder().use { testBuilder ->
        val (deviceId, deviceKey) = testBuilder.manager.devices.setupTestDevice()
        testBuilder.manager.deviceData.setDeviceKey(deviceKey)
        val createdSurvey = testBuilder.manager.surveys.createDefault()
        val layout = testBuilder.manager.layouts.createDefault()
        approveSurvey(createdSurvey)
        val deviceSurvey = testBuilder.manager.deviceSurveys.createCurrentlyPublishedDeviceSurvey(
            deviceId = deviceId,
            surveyId = createdSurvey.id!!
        )

        val multiOptions = (0..2).map {
            PageQuestionOption(
                orderNumber = it,
                questionOptionValue = "Option $it"
            )
        }.toTypedArray()

        //Create one page with single select question
        val page = testBuilder.manager.pages.create(
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
        val answerOption0 = page.question!!.options.find { it.orderNumber == multiOptions[0].orderNumber }!!
        val answerOption1 = page.question.options.find { it.orderNumber == multiOptions[1].orderNumber }!!
        val answerOption2 = page.question.options.find { it.orderNumber == multiOptions[2].orderNumber }!!

        val answerString0 = jacksonObjectMapper().writeValueAsString(
            listOf(
                answerOption0.id,
                answerOption1.id
            )
        )

        createPageAnswer(
            testBuilder = testBuilder,
            deviceSurvey = deviceSurvey,
            page = page,
            answer = answerString0
        )

        val answerString1 = jacksonObjectMapper().writeValueAsString(
            listOf(
                answerOption0.id,
                answerOption2.id
            )
        )

        createPageAnswer(
            testBuilder = testBuilder,
            deviceSurvey = deviceSurvey,
            page = page,
            answer = answerString1
        )

        val answers = testBuilder.manager.surveyAnswers.list(
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
        testBuilder.manager.deviceData.assertCreateFail(
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

    @Test
    fun submitInvalidAnswers() = createTestBuilder().use { testBuilder ->
        val (deviceId, deviceKey) = testBuilder.manager.devices.setupTestDevice()
        testBuilder.manager.deviceData.setDeviceKey(deviceKey)
        val createdSurvey = testBuilder.manager.surveys.createDefault()
        val createdSurvey2 = testBuilder.manager.surveys.createDefault()
        val layout = testBuilder.manager.layouts.createDefault()
        approveSurvey(createdSurvey)
        approveSurvey(createdSurvey2)

        val deviceSurvey1 = testBuilder.manager.deviceSurveys.createCurrentlyPublishedDeviceSurvey(
            deviceId = deviceId,
            surveyId = createdSurvey.id!!,
            addClosable = false // Device survey is cleaned along with the page
        )

        val deviceSurvey2 = testBuilder.manager.deviceSurveys.createCurrentlyPublishedDeviceSurvey(
            deviceId = deviceId,
            surveyId = createdSurvey2.id!!
        )

        //Create one page with freetext answer
        val page = testBuilder.manager.pages.create(
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
        testBuilder.manager.deviceData.assertCreateFail(
            deviceId = randomUUID,
            deviceSurveyId = deviceSurvey1.id!!,
            pageId = page.id!!,
            devicePageSurveyAnswer = DevicePageSurveyAnswer(
                pageId = page.id,
                answer = "qwerty"
            ),
            expectedStatusCode = 401
        )

        testBuilder.manager.deviceData.assertCreateFail(
            deviceId = deviceId,
            deviceSurveyId = deviceSurvey1.id,
            pageId = randomUUID,
            devicePageSurveyAnswer = DevicePageSurveyAnswer(
                pageId = page.id,
                answer = "qwerty"
            ),
            expectedStatusCode = 404
        )

        testBuilder.manager.deviceData.assertCreateFail(
            deviceId = deviceId,
            deviceSurveyId = randomUUID,
            pageId = page.id,
            devicePageSurveyAnswer = DevicePageSurveyAnswer(
                pageId = page.id,
                answer = "qwerty"
            ),
            expectedStatusCode = 404
        )

        testBuilder.manager.deviceData.assertCreateFail(
            deviceId = deviceId,
            deviceSurveyId = deviceSurvey1.id,
            pageId = page.id,
            devicePageSurveyAnswer = DevicePageSurveyAnswer(
                pageId = randomUUID,
                answer = "qwerty"
            ),
            expectedStatusCode = 404
        )

        // cannot answer for survey that the page does not belong to
        testBuilder.manager.deviceData.assertCreateFail(
            deviceId = deviceId,
            deviceSurveyId = deviceSurvey2.id!!,
            pageId = page.id,
            devicePageSurveyAnswer = DevicePageSurveyAnswer(
                pageId = page.id,
                answer = "qwerty"
            ),
            expectedStatusCode = 404
        )

        testBuilder.manager.deviceData.setDeviceKey("invalid key")
        testBuilder.manager.deviceData.assertCreateFail(
            deviceId = deviceId,
            deviceSurveyId = deviceSurvey1.id,
            pageId = page.id,
            devicePageSurveyAnswer = DevicePageSurveyAnswer(
                pageId = page.id,
                answer = "qwerty"
            ),
            expectedStatusCode = 401
        )
        testBuilder.manager.deviceData.setDeviceKey(deviceKey)
    }

    /**
     * Tests whether duplicate answer detection works with text answers
     */
    @Test
    fun testSubmitDuplicateTextAnswers() = createTestBuilder().use { testBuilder ->
        // Setup two devices with same survey published

        val devices = arrayOf(
            testBuilder.manager.devices.setupTestDevice(serialNumber = "1234"),
            testBuilder.manager.devices.setupTestDevice(serialNumber = "5678")
        )

        val createdSurvey = testBuilder.manager.surveys.createDefault()
        val layout = testBuilder.manager.layouts.createDefault()
        approveSurvey(createdSurvey)

        // Two pages with text questions
        val pages = arrayOf("best beer in the world?", "best beer in the universe?").map { title ->
            testBuilder.manager.pages.create(
                surveyId = createdSurvey.id!!,
                page = Page(
                    orderNumber = 0,
                    title = title,
                    question = PageQuestion(
                        type = PageQuestionType.FREETEXT,
                        options = emptyArray()
                    ),
                    layoutId = layout.id!!,
                    nextButtonVisible = true
                )
            )
        }

        for (device in devices) {
            val (deviceId, deviceKey) = device

            testBuilder.manager.deviceData.setDeviceKey(deviceKey)

            val deviceSurvey = testBuilder.manager.deviceSurveys.createCurrentlyPublishedDeviceSurvey(
                deviceId = deviceId,
                surveyId = createdSurvey.id!!
            )

            for (page in pages) {
                // Submit answer 1 three times (without deviceAnswerId simulating direct submission using v1 API)

                for (i in 1..3) {
                    createPageAnswer(
                        testBuilder = testBuilder,
                        deviceSurvey = deviceSurvey,
                        page = page,
                        answer = "Hoptinen Illuusio",
                        deviceAnswerId = null
                    )
                }

                // Submit answer 2 three times (with deviceAnswerId simulating postponed submission from device using v2 API)
                for (i in 1..3) {
                    createPageAnswer(
                        testBuilder = testBuilder,
                        deviceId = deviceId,
                        surveyId = createdSurvey.id,
                        page = page,
                        answer = "Sumurai",
                        deviceAnswerId = 5
                    )
                }

                // Submit answer 3 three times (with deviceAnswerId simulating postponed submission from device using v2 API)
                for (i in 1..3) {
                    createPageAnswer(
                        testBuilder = testBuilder,
                        deviceId = deviceId,
                        page = page,
                        surveyId = createdSurvey.id,
                        answer = "Pyöveli",
                        deviceAnswerId = 6
                    )
                }
            }
        }

        // Assert that both devices have expected answers
        for (page in pages) {
            val answers = testBuilder.manager.surveyAnswers.list(
                surveyId = createdSurvey.id!!,
                pageId = page.id!!
            )

            // Assert that there is three answers from both devices with answer "Hoptinen Illuusio"
            assertEquals(6, answers.filter { it.answer == "Hoptinen Illuusio" }.size)

            // ... and one with answer "Sumurai" from both devices
            assertEquals(2, answers.filter { it.answer == "Sumurai" }.size)

            // ...and one with answer "Pyöveli" from both devices
            assertEquals(2, answers.filter { it.answer == "Pyöveli" }.size)
        }
    }

    /**
     * Tests whether duplicate answer detection works with single select answers
     */
    @Test
    fun testSubmitDuplicateSingleSelectAnswers() = createTestBuilder().use { testBuilder ->
        // Setup two devices with same survey published

        val devices = arrayOf(
            testBuilder.manager.devices.setupTestDevice(serialNumber = "1234"),
            testBuilder.manager.devices.setupTestDevice(serialNumber = "5678")
        )

        val createdSurvey = testBuilder.manager.surveys.createDefault()
        val layout = testBuilder.manager.layouts.createDefault()
        approveSurvey(createdSurvey)

        val singleOptions = (0..2).map {
            PageQuestionOption(
                orderNumber = it,
                questionOptionValue = "Option $it"
            )
        }.toTypedArray()

        val pages = arrayOf("best beer in the world?", "best beer in the universe?").map { title ->
            testBuilder.manager.pages.create(
                surveyId = createdSurvey.id!!,
                page = Page(
                    orderNumber = 0,
                    title = title,
                    question = PageQuestion(
                        type = PageQuestionType.SINGLE_SELECT,
                        options = singleOptions
                    ),
                    layoutId = layout.id!!,
                    nextButtonVisible = true
                )
            )
        }

        for (device in devices) {
            val (deviceId, deviceKey) = device

            testBuilder.manager.deviceData.setDeviceKey(deviceKey)

            val deviceSurvey = testBuilder.manager.deviceSurveys.createCurrentlyPublishedDeviceSurvey(
                deviceId = deviceId,
                surveyId = createdSurvey.id!!
            )

            for (page in pages) {
                // Submit answer 1 three times (without deviceAnswerId simulating direct submission using v1 API)

                for (i in 1..3) {
                    createPageAnswer(
                        testBuilder = testBuilder,
                        deviceSurvey = deviceSurvey,
                        page = page,
                        answer = getOptionValueByOrderNumber(page = page, orderNumber = 0),
                        deviceAnswerId = null
                    )
                }

                // Submit answer 2 three times (with deviceAnswerId simulating postponed submission from device using v2 API)
                for (i in 1..3) {
                    createPageAnswer(
                        testBuilder = testBuilder,
                        deviceId = deviceId,
                        page = page,
                        answer = getOptionValueByOrderNumber(page = page, orderNumber = 1),
                        surveyId = createdSurvey.id,
                        deviceAnswerId = 5
                    )
                }

                // Submit answer 3 three times (with deviceAnswerId simulating postponed submission from device using v2 API)
                for (i in 1..3) {
                    createPageAnswer(
                        testBuilder = testBuilder,
                        deviceId = deviceId,
                        page = page,
                        answer = getOptionValueByOrderNumber(page = page, orderNumber = 2),
                        surveyId = createdSurvey.id,
                        deviceAnswerId = 6
                    )
                }
            }
        }

        // Assert that both devices have expected answers
        for (page in pages) {
            val answers = testBuilder.manager.surveyAnswers.list(
                surveyId = createdSurvey.id!!,
                pageId = page.id!!
            )

            // Assert that there is three answers from both devices with answer 0
            assertEquals(6, answers.filter { it.answer == getOptionValueByOrderNumber(page = page, orderNumber = 0) }.size)

            // ... and one with answer 1 from both devices
            assertEquals(2, answers.filter { it.answer == getOptionValueByOrderNumber(page = page, orderNumber = 1) }.size)

            // ...and one with answer 2 from both devices
            assertEquals(2, answers.filter { it.answer == getOptionValueByOrderNumber(page = page, orderNumber = 2) }.size)
        }
    }

    /**
     * Tests whether duplicate answer detection works with multi select answers
     */
    @Test
    fun testSubmitDuplicateMultiSelectAnswers() = createTestBuilder().use { testBuilder ->
        // Setup two devices with same survey published

        val devices = arrayOf(
            testBuilder.manager.devices.setupTestDevice(serialNumber = "1234"),
            testBuilder.manager.devices.setupTestDevice(serialNumber = "5678")
        )

        val createdSurvey = testBuilder.manager.surveys.createDefault()
        val layout = testBuilder.manager.layouts.createDefault()
        approveSurvey(createdSurvey)

        val singleOptions = (0..2).map {
            PageQuestionOption(
                orderNumber = it,
                questionOptionValue = "Option $it"
            )
        }.toTypedArray()

        val pages = arrayOf("best beer in the world?", "best beer in the universe?").map { title ->
            testBuilder.manager.pages.create(
                surveyId = createdSurvey.id!!,
                page = Page(
                    orderNumber = 0,
                    title = title,
                    question = PageQuestion(
                        type = PageQuestionType.MULTI_SELECT,
                        options = singleOptions
                    ),
                    layoutId = layout.id!!,
                    nextButtonVisible = true
                )
            )
        }

        for (device in devices) {
            val (deviceId, deviceKey) = device

            testBuilder.manager.deviceData.setDeviceKey(deviceKey)

            val deviceSurvey = testBuilder.manager.deviceSurveys.createCurrentlyPublishedDeviceSurvey(
                deviceId = deviceId,
                surveyId = createdSurvey.id!!
            )

            for (page in pages) {
                // Submit answer 1 three times (without deviceAnswerId simulating direct submission using v1 API)

                for (i in 1..3) {
                    createPageAnswer(
                        testBuilder = testBuilder,
                        deviceSurvey = deviceSurvey,
                        page = page,
                        answer = jacksonObjectMapper().writeValueAsString(listOf(
                            getOptionValueByOrderNumber(page = page, orderNumber = 0),
                            getOptionValueByOrderNumber(page = page, orderNumber = 1)
                        )),
                        deviceAnswerId = null
                    )
                }

                // Submit answer 2 three times (with deviceAnswerId simulating postponed submission from device using v2 API)
                for (i in 1..3) {
                    createPageAnswer(
                        testBuilder = testBuilder,
                        deviceId = deviceId,
                        page = page,
                        surveyId = createdSurvey.id,
                        answer = jacksonObjectMapper().writeValueAsString(listOf(
                            getOptionValueByOrderNumber(page = page, orderNumber = 1)
                        )),
                        deviceAnswerId = 5
                    )
                }

                // Submit answer 3 three times (with deviceAnswerId simulating postponed submission from device using v2 API)
                for (i in 1..3) {
                    createPageAnswer(
                        testBuilder = testBuilder,
                        deviceId = deviceId,
                        surveyId = createdSurvey.id,
                        page = page,
                        answer = jacksonObjectMapper().writeValueAsString(listOf(
                            getOptionValueByOrderNumber(page = page, orderNumber = 0),
                            getOptionValueByOrderNumber(page = page, orderNumber = 2)
                        )),
                        deviceAnswerId = 6
                    )
                }
            }
        }

        // Assert that both devices have expected answers
        for (page in pages) {
            val answers = testBuilder.manager.surveyAnswers.list(
                surveyId = createdSurvey.id!!,
                pageId = page.id!!
            )

            val answerValues = answers.map { jacksonObjectMapper().readValue<Array<String>>(it.answer!!) }

            // Assert that there is three answers from both devices with answer 0
            assertEquals(6, countMultiValues(page = page, answerValues = answerValues, orderNumbers = arrayOf(0, 1)))

            // ... and one with answer 1 from both devices
            assertEquals(2, countMultiValues(page = page, answerValues = answerValues, orderNumbers = arrayOf(1)))

            // ...and one with answer 2 from both devices
            assertEquals(2, countMultiValues(page = page, answerValues = answerValues, orderNumbers = arrayOf(0, 2)))
        }
    }

    @Test
    fun testSubmitFreeTextAnswerWithTimestamp() = createTestBuilder().use { testBuilder ->
        val (deviceId, deviceKey) = testBuilder.manager.devices.setupTestDevice(serialNumber = "1234")

        testBuilder.manager.deviceData.setDeviceKey(deviceKey)

        val survey = testBuilder.manager.surveys.createDefault()
        val layout = testBuilder.manager.layouts.createDefault()

        approveSurvey(survey)

        val deviceSurvey = testBuilder.manager.deviceSurveys.createCurrentlyPublishedDeviceSurvey(
            deviceId = deviceId,
            surveyId = survey.id!!
        )
        val page = testBuilder.manager.pages.create(
            surveyId = survey.id,
            page = Page(
                orderNumber = 0,
                title = "Question title",
                question = PageQuestion(
                    type = PageQuestionType.FREETEXT,
                    options = emptyArray()
                ),
                layoutId = layout.id!!,
                nextButtonVisible = false
            )
        )
        val monthAgo = OffsetDateTime.now().minusMonths(1)

        // Submit an answer with timestamp via the V2 API (simulate postponed submission from device)
        createPageAnswer(
            testBuilder = testBuilder,
            deviceId = deviceId,
            page = page,
            surveyId = survey.id,
            answer = "free text answer",
            deviceAnswerId = 1,
            overrideCreatedAt = monthAgo
        )
        val (answer) = testBuilder.manager.surveyAnswers.list(
            surveyId = survey.id,
            pageId = page.id!!
        )

        // Assert that the answers createdAt timestamp is the same as the one provided
        assertOffsetDateTimeEquals(monthAgo, OffsetDateTime.parse(answer.metadata!!.createdAt))

        // Submit an answer without timestamp via the V1 API (simulate postponed submission from device)
        createPageAnswer(
            testBuilder = testBuilder,
            deviceSurvey = deviceSurvey,
            page = page,
            answer = "Hoptinen Illuusio",
            deviceAnswerId = null
        )
        val (answer2) = testBuilder.manager.surveyAnswers.list(
            surveyId = survey.id,
            pageId = page.id
        ).filter { it.id != answer.id }

        // Assert that the answers createdAt timestamp is set to the time of the submission when no timestamp is provided.
        // Uses LocalDate comparison to avoid second precision problems
        assertOffsetDateTimeEquals(OffsetDateTime.now(), OffsetDateTime.parse(answer2.metadata!!.createdAt))
    }

    @Test
    fun testSubmitSingleSelectAnswerWithTimestamp() = createTestBuilder().use { testBuilder ->
        val (deviceId, deviceKey) = testBuilder.manager.devices.setupTestDevice(serialNumber = "1234")

        testBuilder.manager.deviceData.setDeviceKey(deviceKey)

        val survey = testBuilder.manager.surveys.createDefault()
        val layout = testBuilder.manager.layouts.createDefault()

        approveSurvey(survey)

        val deviceSurvey = testBuilder.manager.deviceSurveys.createCurrentlyPublishedDeviceSurvey(
            deviceId = deviceId,
            surveyId = survey.id!!
        )

        val page = testBuilder.manager.pages.create(
            surveyId = survey.id,
            page = Page(
                orderNumber = 0,
                title = "Question title",
                question = PageQuestion(
                    type = PageQuestionType.SINGLE_SELECT,
                    options = arrayOf(PageQuestionOption(orderNumber = 0, questionOptionValue = "option 1"))
                ),
                layoutId = layout.id!!,
                nextButtonVisible = false
            )
        )
        val monthAgo = OffsetDateTime.now().minusMonths(1)

        // Submit an answer with timestamp via the V2 API (simulate postponed submission from device)
        createPageAnswer(
            testBuilder = testBuilder,
            deviceId = deviceId,
            page = page,
            surveyId = survey.id,
            answer = getOptionValueByOrderNumber(page = page, orderNumber = 0),
            deviceAnswerId = 1,
            overrideCreatedAt = monthAgo
        )
        val (answer) = testBuilder.manager.surveyAnswers.list(
            surveyId = survey.id,
            pageId = page.id!!
        )

        // Assert that the answers createdAt timestamp is the same as the one provided
        assertOffsetDateTimeEquals(monthAgo, OffsetDateTime.parse(answer.metadata!!.createdAt))

        // Submit an answer without timestamp via the V1 API (simulate postponed submission from device)
        createPageAnswer(
            testBuilder = testBuilder,
            deviceSurvey = deviceSurvey,
            page = page,
            answer = getOptionValueByOrderNumber(page = page, orderNumber = 0),
            deviceAnswerId = null
        )
        val (answer2) = testBuilder.manager.surveyAnswers.list(
            surveyId = survey.id,
            pageId = page.id
        ).filter { it.id != answer.id }

        // Assert that the answers createdAt timestamp is set to the time of the submission when no timestamp is provided.
        // Uses LocalDate comparison to avoid second precision problems
        assertOffsetDateTimeEquals(OffsetDateTime.now(), OffsetDateTime.parse(answer2.metadata!!.createdAt))
    }

    @Test
    fun testSubmitMultiSelectAnswerWithTimestamp() = createTestBuilder().use { testBuilder ->
        val (deviceId, deviceKey) = testBuilder.manager.devices.setupTestDevice(serialNumber = "1234")

        testBuilder.manager.deviceData.setDeviceKey(deviceKey)

        val survey = testBuilder.manager.surveys.createDefault()
        val layout = testBuilder.manager.layouts.createDefault()

        approveSurvey(survey)

        val deviceSurvey = testBuilder.manager.deviceSurveys.createCurrentlyPublishedDeviceSurvey(
            deviceId = deviceId,
            surveyId = survey.id!!
        )

        val page = testBuilder.manager.pages.create(
            surveyId = survey.id,
            page = Page(
                orderNumber = 0,
                title = "Question title",
                question = PageQuestion(
                    type = PageQuestionType.MULTI_SELECT,
                    options = arrayOf(PageQuestionOption(orderNumber = 0, questionOptionValue = "option 1"))
                ),
                layoutId = layout.id!!,
                nextButtonVisible = false
            )
        )
        val monthAgo = OffsetDateTime.now().minusMonths(1)

        // Submit an answer with timestamp via the V2 API (simulate postponed submission from device)
        createPageAnswer(
            testBuilder = testBuilder,
            deviceId = deviceId,
            page = page,
            surveyId = survey.id,
            answer = jacksonObjectMapper().writeValueAsString(listOf(getOptionValueByOrderNumber(page = page, orderNumber = 0))),
            deviceAnswerId = 1,
            overrideCreatedAt = monthAgo
        )
        val (answer) = testBuilder.manager.surveyAnswers.list(
            surveyId = survey.id,
            pageId = page.id!!
        )

        // Assert that the answers createdAt timestamp is the same as the one provided
        assertOffsetDateTimeEquals(monthAgo, OffsetDateTime.parse(answer.metadata!!.createdAt))

        // Submit an answer without timestamp via the V1 API (simulate postponed submission from device)
        createPageAnswer(
            testBuilder = testBuilder,
            deviceSurvey = deviceSurvey,
            page = page,
            answer = jacksonObjectMapper().writeValueAsString(listOf(getOptionValueByOrderNumber(page = page, orderNumber = 0))),
            deviceAnswerId = null
        )
        val (answer2) = testBuilder.manager.surveyAnswers.list(
            surveyId = survey.id,
            pageId = page.id
        ).filter { it.id != answer.id }

        // Assert that the answers createdAt timestamp is set to the time of the submission when no timestamp is provided.
        // Uses LocalDate comparison to avoid second precision problems
        assertOffsetDateTimeEquals(OffsetDateTime.now(), OffsetDateTime.parse(answer2.metadata!!.createdAt))
    }

    @Test
    fun testSubmitMultiSelectAnswerWithoutAnswerOptionId() = createTestBuilder().use { testBuilder ->
        val (deviceId, deviceKey) = testBuilder.manager.devices.setupTestDevice(serialNumber = "1234")

        testBuilder.manager.deviceData.setDeviceKey(deviceKey)

        val survey = testBuilder.manager.surveys.createDefault()
        val layout = testBuilder.manager.layouts.createDefault()

        approveSurvey(survey)

        val page = testBuilder.manager.pages.create(
            surveyId = survey.id!!,
            page = Page(
                orderNumber = 0,
                title = "Question title",
                question = PageQuestion(
                    type = PageQuestionType.MULTI_SELECT,
                    options = arrayOf(PageQuestionOption(orderNumber = 0, questionOptionValue = "option 1"))
                ),
                layoutId = layout.id!!,
                nextButtonVisible = false
            )
        )

        // Submit an answer without answer via the V2 API (simulate postponed submission from device with malformed answer data)
        createPageAnswer(
            testBuilder = testBuilder,
            deviceId = deviceId,
            page = page,
            surveyId = survey.id,
            answer = null,
            deviceAnswerId = 1
        )
        val (answer) = testBuilder.manager.surveyAnswers.list(
            surveyId = survey.id,
            pageId = page.id!!
        )
        val pageWithFailsafeOption = testBuilder.manager.pages.find(surveyId = survey.id, pageId = page.id)
        val failsafeOption = pageWithFailsafeOption.question?.options?.find { it.questionOptionValue == PageQuestionController.FAILSAFE_NO_SELECTION_OPTION_VALUE}
        assertEquals(answer.answer, jacksonObjectMapper().writeValueAsString(listOf(failsafeOption?.id)))
    }

    @Test
    fun testSubmitSingleSelectAnswerWithoutAnswerOptionId() = createTestBuilder().use { testBuilder ->
        val (deviceId, deviceKey) = testBuilder.manager.devices.setupTestDevice(serialNumber = "1234")

        testBuilder.manager.deviceData.setDeviceKey(deviceKey)

        val survey = testBuilder.manager.surveys.createDefault()
        val layout = testBuilder.manager.layouts.createDefault()

        approveSurvey(survey)

        val page = testBuilder.manager.pages.create(
            surveyId = survey.id!!,
            page = Page(
                orderNumber = 0,
                title = "Question title",
                question = PageQuestion(
                    type = PageQuestionType.SINGLE_SELECT,
                    options = arrayOf(PageQuestionOption(orderNumber = 0, questionOptionValue = "option 1"))
                ),
                layoutId = layout.id!!,
                nextButtonVisible = false
            )
        )

        // Submit an answer without answer via the V2 API (simulate postponed submission from device with malformed answer data)
        createPageAnswer(
            testBuilder = testBuilder,
            deviceId = deviceId,
            page = page,
            surveyId = survey.id,
            answer = null,
            deviceAnswerId = 1
        )
        val (answer) = testBuilder.manager.surveyAnswers.list(
            surveyId = survey.id,
            pageId = page.id!!
        )
        val pageWithFailsafeOption = testBuilder.manager.pages.find(surveyId = survey.id, pageId = page.id)
        val failsafeOption = pageWithFailsafeOption.question?.options?.find { it.questionOptionValue == PageQuestionController.FAILSAFE_NO_SELECTION_OPTION_VALUE}
        assertEquals(answer.answer, failsafeOption?.id.toString())
    }

    @Test
    fun testSubmitFreeTextAnswerWithoutAnswerOptionId() = createTestBuilder().use { testBuilder ->
        val (deviceId, deviceKey) = testBuilder.manager.devices.setupTestDevice(serialNumber = "1234")

        testBuilder.manager.deviceData.setDeviceKey(deviceKey)

        val survey = testBuilder.manager.surveys.createDefault()
        val layout = testBuilder.manager.layouts.createDefault()

        approveSurvey(survey)

        val page = testBuilder.manager.pages.create(
            surveyId = survey.id!!,
            page = Page(
                orderNumber = 0,
                title = "Question title",
                question = PageQuestion(
                    type = PageQuestionType.FREETEXT,
                    options = emptyArray()
                ),
                layoutId = layout.id!!,
                nextButtonVisible = false
            )
        )

        // Submit an answer without answer via the V2 API (simulate postponed submission from device with malformed answer data)
        createPageAnswer(
            testBuilder = testBuilder,
            deviceId = deviceId,
            page = page,
            surveyId = survey.id,
            answer = null,
            deviceAnswerId = 1
        )
        val (answer) = testBuilder.manager.surveyAnswers.list(
            surveyId = survey.id,
            pageId = page.id!!
        )

        assertEquals(answer.answer, "")
    }

    /**
     * Counts the amount of answers that contain all the given order numbers
     *
     * @param page page
     * @param answerValues list of answer values
     * @param orderNumbers array of order numbers
     * @return amount of answers that contain all the given order numbers
     */
    private fun countMultiValues(
        page: Page,
        answerValues: List<Array<String>>,
        orderNumbers: Array<Int>
    ): Int {
        val expectedIds = orderNumbers.map { getOptionValueByOrderNumber(page = page, orderNumber = it) }
        return answerValues.filter { it.sorted() == expectedIds.sorted() }.size
    }

    /**
     * Resolves the option value by order number
     *
     * @param page page
     * @param orderNumber order number
     * @return option value
     */
    private fun getOptionValueByOrderNumber(page: Page, orderNumber: Int): String = page.question!!.options.find { it.orderNumber == orderNumber }?.id?.toString()!!


}