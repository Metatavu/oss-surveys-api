package fi.metatavu.oss.api.impl.devicesurveys

import fi.metatavu.oss.api.impl.AbstractApi
import fi.metatavu.oss.api.impl.UserRole
import fi.metatavu.oss.api.impl.devices.DeviceController
import fi.metatavu.oss.api.impl.surveys.SurveyController
import fi.metatavu.oss.api.model.DeviceSurvey
import fi.metatavu.oss.api.model.DeviceSurveyStatus
import fi.metatavu.oss.api.model.SurveyStatus
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import java.util.*
import javax.annotation.security.RolesAllowed
import javax.enterprise.context.RequestScoped
import javax.inject.Inject
import javax.ws.rs.core.Response

@RequestScoped
@Suppress("unused")
@OptIn(ExperimentalCoroutinesApi::class)
class DeviceSurveysApiImpl: fi.metatavu.oss.api.spec.DeviceSurveysApi, AbstractApi() {

    @Inject
    lateinit var deviceSurveyController: DeviceSurveyController

    @Inject
    lateinit var deviceController: DeviceController

    @Inject
    lateinit var surveyController: SurveyController

    @Inject
    lateinit var deviceSurveyTranslator: DeviceSurveyTranslator

    @Inject
    lateinit var vertx: Vertx

    @ReactiveTransactional
    @RolesAllowed(UserRole.MANAGER.name)
    override fun createDeviceSurvey(deviceId: UUID, deviceSurvey: DeviceSurvey): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)

        deviceController.findDevice(deviceId) ?: return@async createNotFoundWithMessage(
            target = DEVICE,
            id = deviceId
        )

        if (deviceId != deviceSurvey.deviceId) {
            return@async createBadRequest("Device id in path and body do not match")
        }

        val foundSurvey = surveyController.findSurvey(deviceSurvey.surveyId) ?: return@async createBadRequest("Survey not found")

        if (foundSurvey.status != SurveyStatus.APPROVED) {
            return@async createBadRequest("Survey is not approved")
        }

        val foundDevice = deviceController.findDevice(deviceSurvey.deviceId) ?: return@async createBadRequest("Device not found")

        if (deviceSurvey.status == DeviceSurveyStatus.SCHEDULED && !deviceSurveyController.validateScheduledDeviceSurvey(deviceSurvey)) {
            return@async createBadRequest("Device survey schedule is not valid")
        }

        if (deviceSurvey.status == DeviceSurveyStatus.PUBLISHED) {
            deviceSurveyController.listDeviceSurveysByDevice(deviceId = deviceId, status = DeviceSurveyStatus.PUBLISHED)
                .first
                .forEach { deviceSurveyController.deleteDeviceSurvey(it) }

        }

        val createdDeviceSurvey = deviceSurveyController.createDeviceSurvey(
            deviceSurvey = deviceSurvey,
            device = foundDevice,
            survey = foundSurvey,
            userId = userId
        )

        createCreated(deviceSurveyTranslator.translate(createdDeviceSurvey))
    }.asUni()

    @ReactiveTransactional
    @RolesAllowed(UserRole.MANAGER.name)
    override fun deleteDeviceSurvey(deviceId: UUID, deviceSurveyId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        deviceController.findDevice(deviceId)
            ?: return@async createNotFoundWithMessage(
                target = DEVICE,
                id = deviceId
            )

        val foundDeviceSurvey = deviceSurveyController
            .findDeviceSurvey(deviceSurveyId)
            ?: return@async createBadRequest("Device survey not found")

        if (deviceId != foundDeviceSurvey.device.id) {
            return@async createBadRequest("Device id in path and body do not match")
        }

        deviceSurveyController.deleteDeviceSurvey(foundDeviceSurvey)

        createNoContent()
    }.asUni()

    @ReactiveTransactional
    @RolesAllowed(UserRole.MANAGER.name)
    override fun findDeviceSurvey(deviceId: UUID, deviceSurveyId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        deviceController.findDevice(deviceId)
            ?: return@async createNotFoundWithMessage(
                target = DEVICE,
                id = deviceId
            )
        val foundDeviceSurvey = deviceSurveyController.findDeviceSurvey(deviceSurveyId) ?: return@async createNotFound("Device survey not found")

        if (deviceId != foundDeviceSurvey.device.id) {
            return@async createBadRequest("Device id in path and body do not match")
        }

        createOk(deviceSurveyTranslator.translate(foundDeviceSurvey))
    }.asUni()

    @RolesAllowed(UserRole.MANAGER.name)
    override fun listDeviceSurveys(deviceId: UUID, firstResult: Int?, maxResults: Int?, status: DeviceSurveyStatus?): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val (deviceSurveys, count) = deviceSurveyController.listDeviceSurveysByDevice(
            deviceId = deviceId,
            firstResult = firstResult,
            maxResults = maxResults,
            status = status
        )
        val deviceSurveysTranslated = deviceSurveyTranslator.translate(deviceSurveys)

        createOk(deviceSurveysTranslated, count)
    }.asUni()

    @ReactiveTransactional
    @RolesAllowed(UserRole.MANAGER.name)
    override fun updateDeviceSurvey(deviceId: UUID, deviceSurveyId: UUID, deviceSurvey: DeviceSurvey): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        deviceController.findDevice(deviceId) ?: return@async createNotFoundWithMessage(
            target = DEVICE,
            id = deviceId
        )
        val foundDeviceSurvey = deviceSurveyController
            .findDeviceSurvey(deviceSurveyId)
            ?: return@async createBadRequest("Device survey not found")

        if (deviceId != deviceSurvey.deviceId) {
            return@async createBadRequest("Device id in path and body do not match")
        }

        if (deviceSurvey.status == DeviceSurveyStatus.SCHEDULED && !deviceSurveyController.validateScheduledDeviceSurvey(deviceSurvey)) {
            return@async createBadRequest("Device survey schedule is not valid")
        }

        if (deviceSurvey.status == DeviceSurveyStatus.PUBLISHED) {
            deviceSurveyController.listDeviceSurveysByDevice(deviceId = deviceId, status = DeviceSurveyStatus.PUBLISHED)
                .first
                .filter { it.id != deviceSurveyId }
                .forEach { deviceSurveyController.deleteDeviceSurvey(it) }

        }

        val updatedDeviceSurvey = deviceSurveyController.updateDeviceSurvey(
            deviceSurveyToUpdate = foundDeviceSurvey,
            newRestDeviceSurvey = deviceSurvey,
            userId = userId
        )

        createOk(deviceSurveyTranslator.translate(updatedDeviceSurvey))
    }.asUni()

}