package fi.metatavu.oss.api.test.functional.resources

import io.quarkus.test.junit.QuarkusTestProfile

/**
 * Scheduler Quarkus test profile
 */
class SchedulerTestProfile: QuarkusTestProfile {

    override fun getConfigOverrides(): Map<String, String> {
        return mapOf(
            "quarkus.scheduler.enabled" to "true",
            "scheduled.survey.publish.interval" to "1s",
        )
    }

}