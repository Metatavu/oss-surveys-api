package fi.metatavu.oss.api.impl.scheduler

import fi.metatavu.oss.api.impl.devicesurveys.DeviceSurveyController
import fi.metatavu.oss.api.impl.devicesurveys.DeviceSurveyEntity
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional
import io.quarkus.scheduler.Scheduled
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.*
import org.slf4j.Logger
import java.util.concurrent.TimeUnit
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.context.control.ActivateRequestContext
import javax.inject.Inject

/**
 * Scheduled survey publisher
 */
@Suppress("unused")
@ApplicationScoped
class ScheduledSurveyPublisher {

    @Inject
    lateinit var vertx: Vertx

    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var deviceSurveyController: DeviceSurveyController

    @OptIn(ExperimentalCoroutinesApi::class)
    @Scheduled(
        every = "\${scheduled.survey.publish.interval}",
        delayed = "\${scheduled.survey.publish.delay}",
        delayUnit = TimeUnit.SECONDS
    )
    @ActivateRequestContext
    @ReactiveTransactional
    fun publishScheduledSurveys(): Uni<Void> = CoroutineScope(vertx.dispatcher()).async {
        val deviceSurveysToPublish = deviceSurveyController.listDeviceSurveysToPublish()
        val deviceSurveysToUnPublish = deviceSurveyController.listDeviceSurveysToUnPublish()

        unPublishDeviceSurveys(deviceSurveysToUnPublish)
        publishDeviceSurveys(deviceSurveysToPublish)
    }.asUni().replaceWithVoid()

    /**
     * Publishes given surveys e.g. updates them and notifies the associated devices
     *
     * @param deviceSurveys device surveys to publish
     */
    private suspend fun publishDeviceSurveys(deviceSurveys: List<DeviceSurveyEntity>) {
        if (deviceSurveys.isEmpty()) {
            logger.info("No device surveys to publish")
            return
        }
        for (deviceSurvey in deviceSurveys) {
            val survey = deviceSurvey.survey
            val device = deviceSurvey.device
            deviceSurveyController.publishDeviceSurvey(deviceSurvey)
            logger.info("Published device survey ${survey.title} (${survey.id}) for device ${device.name} (${device.id})")
        }
    }

    /**
     * Un-publishes given surveys e.g. deletes them and notifies the associated devices
     *
     * @param deviceSurveys device surveys to un-publish
     */
    private suspend fun unPublishDeviceSurveys(deviceSurveys: List<DeviceSurveyEntity>) {
        if (deviceSurveys.isEmpty()) {
            logger.info("No device surveys to un-publish")
            return
        }
        for (deviceSurvey in deviceSurveys) {
            val survey = deviceSurvey.survey
            val device = deviceSurvey.device
            deviceSurveyController.deleteDeviceSurvey(deviceSurvey)
            logger.info("Un-published device survey ${survey.title} (${survey.id}) for device ${device.name} (${device.id})")
        }
    }
}