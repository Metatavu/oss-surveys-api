package fi.metatavu.oss.api.test.functional.tests

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.metatavu.oss.api.test.functional.TestBuilder
import fi.metatavu.oss.api.test.functional.resources.MqttResource
import fi.metatavu.oss.test.client.models.*
import io.quarkus.test.common.DevServicesContext
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import org.eclipse.microprofile.config.ConfigProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Abstract base class for resource tests
 */
@QuarkusTest
@QuarkusTestResource(MqttResource::class)
abstract class AbstractResourceTest {

    private var devServicesContext: DevServicesContext? = null

    /**
     * Asserts offset datetime equals
     *
     * @param expectedDateTime expected date time
     * @param actualDateTime actual date time
     * @param retention retention
     */
    protected fun assertOffsetDateTimeEquals(expectedDateTime: String, actualDateTime: String, retention: ChronoUnit = ChronoUnit.SECONDS) {
        assertEquals(
            OffsetDateTime.parse(expectedDateTime).toInstant().truncatedTo(retention),
            OffsetDateTime.parse(actualDateTime).toInstant().truncatedTo(retention)
        )
    }

    /**
     * Devices are being created as a side effect from the device fetching its key for the first time,
     * therefore devices need special clean up as they cannot be handled as closeables.
     */
    @AfterEach
    fun cleanDevicesAfterEach() {
        createTestBuilder().use {
            it.manager.devices.list().forEach { device ->
                it.manager.devices.delete(device.id!!)
            }
        }
    }

    /**
     * Marks survey as approved
     *
     * @param survey
     */
    fun approveSurvey(survey: Survey) {
        createTestBuilder().use {
            it.manager.surveys.update(
                surveyId = survey.id!!,
                newSurvey = survey.copy(status = SurveyStatus.APPROVED)
            )
        }
    }

    /**
     * Creates an answer for a page
     *
     * @param testBuilder test builder
     * @param deviceSurvey device survey
     * @param page page
     * @param answer answer
     * @param deviceAnswerId device answer id (optional)
     */
    protected fun createPageAnswer(
        testBuilder: TestBuilder,
        deviceSurvey: DeviceSurvey,
        page: Page,
        answer: String,
        deviceAnswerId: Long? = null
    ) {
        testBuilder.manager.deviceData.submitSurveyAnswer(
            deviceId = deviceSurvey.deviceId,
            deviceSurveyId = deviceSurvey.id!!,
            pageId = page.id!!,
            devicePageSurveyAnswer = DevicePageSurveyAnswer(
                pageId = page.id,
                answer = answer,
                deviceAnswerId = deviceAnswerId
            ),
            surveyId = deviceSurvey.surveyId
        )
    }

    /**
     * Creates an answer for a page using V2 API
     *
     * @param testBuilder test builder
     * @param deviceId device id
     * @param page page
     * @param answer answer
     * @param deviceAnswerId device answer id (optional
     * @param surveyId survey id
     */
    protected fun createPageAnswer(
        testBuilder: TestBuilder,
        deviceId: UUID,
        page: Page,
        answer: String,
        deviceAnswerId: Long? = null,
        surveyId: UUID,
        timestamp: OffsetDateTime? = null
    ) {
        testBuilder.manager.deviceData.submitSurveyAnswer(
            deviceId = deviceId,
            devicePageSurveyAnswer = DevicePageSurveyAnswer(
                pageId = page.id,
                answer = answer,
                deviceAnswerId = deviceAnswerId,
                timestamp = timestamp?.toEpochSecond()
            ),
            surveyId = surveyId,
            pageId = page.id!!
        )
    }

    /**
     * Creates a single select answer for a page
     *
     * @param testBuilder test builder
     * @param deviceSurvey device survey
     * @param page page
     * @param optionIndex option index
     */
    protected fun createSingleSelectAnswer(
        testBuilder: TestBuilder,
        deviceSurvey: DeviceSurvey,
        page: Page,
        optionIndex: Int
    ) {
        createPageAnswer(
            testBuilder = testBuilder,
            deviceSurvey = deviceSurvey,
            page = page,
            answer = page.question!!.options.find { it.orderNumber == optionIndex }!!.id.toString()
        )
    }

    /**
     * Creates a multi select answer for a page
     *
     * @param testBuilder test builder
     * @param deviceSurvey device survey
     * @param page page
     * @param optionIndices option indices
     */
    protected fun createMultiSelectAnswer(
        testBuilder: TestBuilder,
        deviceSurvey: DeviceSurvey,
        page: Page,
        optionIndices: Array<Int>
    ) {
        val answerIds = page.question!!.options
            .filter { optionIndices.contains(it.orderNumber) }
            .mapNotNull { it.id }

        val answer = jacksonObjectMapper().writeValueAsString(answerIds)

        createPageAnswer(
            testBuilder = testBuilder,
            deviceSurvey = deviceSurvey,
            page = page,
            answer = answer
        )
    }

    /**
     * Creates new test builder
     *
     * @return new test builder
     */
    protected fun createTestBuilder(): TestBuilder {
        return TestBuilder(getConfig())
    }

    /**
     * Returns config for tests.
     *
     * If tests are running in native mode, method returns config from devServicesContext and
     * when tests are running in JVM mode method returns config from the Quarkus config
     *
     * @return config for tests
     */
    private fun getConfig(): Map<String, String> {
        return getDevServiceConfig() ?: getQuarkusConfig()
    }

    /**
     * Returns test config from dev services
     *
     * @return test config from dev services
     */
    private fun getDevServiceConfig(): Map<String, String>? {
        return devServicesContext?.devServicesProperties()
    }

    /**
     * Returns test config from Quarkus
     *
     * @return test config from Quarkus
     */
    private fun getQuarkusConfig(): Map<String, String> {
        val config = ConfigProvider.getConfig()
        return config.propertyNames.associateWith { config.getConfigValue(it).rawValue }
    }

}