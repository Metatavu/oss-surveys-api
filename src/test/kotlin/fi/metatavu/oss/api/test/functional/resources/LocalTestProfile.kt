package fi.metatavu.oss.api.test.functional.resources

import io.quarkus.test.junit.QuarkusTestProfile

/**
 * Local Quarkus test profile
 */
class LocalTestProfile: QuarkusTestProfile {

    override fun getConfigOverrides(): Map<String, String> {
        return mapOf(
            "quarkus.scheduler.enabled" to "false",
            "scheduled.survey.publish.interval" to "5s",
            "environment" to "test"
        )
    }

}