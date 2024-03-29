package fi.metatavu.oss.api.test.functional.resources

import io.quarkus.test.junit.QuarkusTestProfile

/**
 * Local Quarkus test profile
 */
open class LocalTestProfile: QuarkusTestProfile {

    protected open val schedulerEnabled = "false"

    override fun getConfigOverrides(): Map<String, String> {
        return mapOf(
            "quarkus.scheduler.enabled" to "true",
            "scheduled.survey.publish.interval" to "5s",
            "environment" to "test",
            "mqtt.base.topic" to "test",
            "scheduled.survey.publish.delay" to "5s",
            "quarkus.scheduler.enabled" to schedulerEnabled
        )
    }

}