package fi.metatavu.oss.api.impl.scheduler

import fi.metatavu.oss.api.impl.devicesurveys.DeviceSurveyController
import fi.metatavu.oss.api.model.DeviceSurveyStatus
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

    @Suppress("unused")
    @OptIn(ExperimentalCoroutinesApi::class)
    @Scheduled(
        every = "\${scheduled.survey.publish.interval}",
        delay = 30,
        delayUnit = TimeUnit.SECONDS
    )
    @ReactiveTransactional
    fun publishScheduledSurveys(): Uni<Void> {
         return CoroutineScope(vertx.dispatcher()).async {
            val deviceSurveysToPublish = deviceSurveyController.listDeviceSurveysToPublish()

            logger.info("Publishing scheduled surveys...")
            for (deviceSurvey in deviceSurveysToPublish) {
                val (existingDeviceSurveys) = deviceSurveyController.listDeviceSurveysByDevice(
                    deviceId = deviceSurvey.device.id,
                    status = DeviceSurveyStatus.PUBLISHED
                )
                logger.info("Un-publishing existing device surveys for ${deviceSurvey.device.id}...")
                for (existingDeviceSurvey in existingDeviceSurveys) {
                    deviceSurveyController.unPublishDeviceSurvey(existingDeviceSurvey)
                    logger.info("Un-published existing device survey ${existingDeviceSurvey.id}")
                }
                logger.info("Publishing scheduled survey ${deviceSurvey.id}")
                deviceSurveyController.publishDeviceSurvey(deviceSurvey)
            }
        }.asUni().replaceWithVoid()
    }
}