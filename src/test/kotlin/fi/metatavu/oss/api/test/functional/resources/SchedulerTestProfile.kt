package fi.metatavu.oss.api.test.functional.resources

/**
 * Quarkus test profile that enabled scheduler.
 *
 * It is disabled by default to prevent inconsistency.
 */
class SchedulerTestProfile: LocalTestProfile() {

    override val schedulerEnabled = "true"
}