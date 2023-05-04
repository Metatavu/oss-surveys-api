package fi.metatavu.oss.api.test.functional.tests

import fi.metatavu.oss.api.test.functional.resources.LocalTestProfile
import fi.metatavu.oss.test.client.models.DeviceSurvey
import fi.metatavu.oss.test.client.models.DeviceSurveyStatus
import fi.metatavu.oss.test.client.models.SurveyStatus
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import org.awaitility.Awaitility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.OffsetDateTime

/**
 * Tests for Scheduler
 */
@QuarkusTest
@TestProfile(LocalTestProfile::class)
class SchedulerTestIT: AbstractResourceTest() {

    @Test
    fun testScheduler() = createTestBuilder().use { testBuilder ->
        val (deviceId) = testBuilder.manager.deviceSurveys.setupTestDevice()
        val createdSurvey = testBuilder.manager.surveys.createDefault()
        testBuilder.manager.surveys.update(
            surveyId = createdSurvey.id!!,
            newSurvey = createdSurvey.copy(status = SurveyStatus.APPROVED)
        )

        testBuilder.manager.deviceSurveys.create(
            deviceId = deviceId,
            deviceSurvey = DeviceSurvey(
                deviceId = deviceId,
                surveyId = createdSurvey.id,
                status = DeviceSurveyStatus.SCHEDULED,
                publishStartTime = OffsetDateTime.now().plusSeconds(5).toString(),
                publishEndTime = OffsetDateTime.now().plusMinutes(1).toString()
            )
        )

        val foundDeviceSurveys = testBuilder.manager.deviceSurveys.list(
            deviceId = deviceId,
            status = DeviceSurveyStatus.SCHEDULED
        )

        assertEquals(1, foundDeviceSurveys.size)

        Awaitility
            .await()
            .atMost(Duration.ofMinutes(1))
            .pollInterval(Duration.ofSeconds(5))
            .until {
                testBuilder.manager.deviceSurveys.list(
                    deviceId = deviceId,
                    status = DeviceSurveyStatus.PUBLISHED
                ).isNotEmpty()
            }
    }
}