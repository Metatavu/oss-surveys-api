package fi.metatavu.oss.api.impl.scheduler

import fi.metatavu.oss.api.impl.devicesurveys.DeviceSurveyController
import fi.metatavu.oss.api.model.DeviceSurveyStatus
import io.quarkus.hibernate.reactive.panache.Panache
import io.quarkus.scheduler.Scheduled
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.*
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.Logger
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Scheduled survey publisher
 */
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
    @Scheduled(every = "\${scheduled.survey.publish.interval}")
    fun publishScheduledSurveys() {
//         CoroutineScope(vertx.dispatcher()).launch {
//            val deviceSurveysToPublish = deviceSurveyController.listDeviceSurveysToPublish()
//            val deviceSurveysToUnPublish = deviceSurveyController.listDeviceSurveysToUnPublish()
//
//            logger.info("Publishing scheduled surveys...")
//            for (deviceSurvey in deviceSurveysToPublish) {
//                Panache.withTransaction {
//                    async {
//                        val (existingDeviceSurveys) = deviceSurveyController.listDeviceSurveys(
//                            deviceId = deviceSurvey.device.id,
//                            firstResult = null,
//                            maxResults = null,
//                            status = DeviceSurveyStatus.PUBLISHED
//                        )
//                        logger.info("Un-publishing existing device surveys for ${deviceSurvey.device.id}...")
//                        for (existingDeviceSurvey in existingDeviceSurveys) {
//                            deviceSurveyController.unPublishDeviceSurvey(existingDeviceSurvey)
//                            logger.info("Un-published existing device survey ${existingDeviceSurvey.id}")
//                        }
//                        logger.info("Publishing scheduled survey ${deviceSurvey.id}")
//                        deviceSurveyController.publishDeviceSurvey(deviceSurvey)
//                    }.asUni()
//                }
//            }
//        }
    }
}