package fi.metatavu.oss.api.impl.devices

import fi.metatavu.oss.api.impl.devicesurveys.DeviceSurveyController
import fi.metatavu.oss.api.impl.pages.answers.PageAnswerController
import fi.metatavu.oss.api.impl.requests.DeviceRequestEntity
import fi.metatavu.oss.api.model.DeviceStatus
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.security.PublicKey
import java.time.OffsetDateTime
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Controller class for devices
 */
@ApplicationScoped
class DeviceController {

    @Inject
    lateinit var deviceRepository: DeviceRepository

    @Inject
    lateinit var deviceSurveyController: DeviceSurveyController

    @Inject
    lateinit var answerController: PageAnswerController

    /**
     * Creates a device
     *
     * @param deviceRequest device request
     * @param deviceKey device key
     * @param userId user id
     * @return created device
     */
    suspend fun createDevice(
        deviceRequest: DeviceRequestEntity,
        deviceKey: PublicKey,
        userId: UUID
    ): DeviceEntity {
        val newDevice = DeviceEntity()
        newDevice.id = deviceRequest.id
        newDevice.serialNumber = deviceRequest.serialNumber
        newDevice.deviceKey = deviceKey.encoded
        newDevice.deviceStatus = DeviceStatus.OFFLINE
        newDevice.name = deviceRequest.name
        newDevice.description = deviceRequest.description
        newDevice.location = deviceRequest.location
        newDevice.creatorId = userId
        newDevice.lastModifierId = userId
        newDevice.lastSeen = OffsetDateTime.now()

        return deviceRepository.create(device = newDevice)
    }

    /**
     * Gets device private key by device id
     *
     * @param id id
     * @return device private key base 64 encoded
     */
    suspend fun getDeviceKey(id: UUID): ByteArray? {
        return deviceRepository.findById(id).awaitSuspending()?.deviceKey
    }

    /**
     * Finds a Device
     *
     * @param id id
     * @return found device
     */
    suspend fun findDevice(id: UUID): DeviceEntity? {
        return deviceRepository.findById(id).awaitSuspending()
    }

    /**
     * Deletes a Device
     *
     * Also deletes associated device surveys
     *
     * @param device device
     */
    suspend fun deleteDevice(device: DeviceEntity) {
        answerController.list(device).forEach {
            answerController.unassignFromDevice(it)
        }
        val (deviceSurveys) = deviceSurveyController.listDeviceSurveysByDevice(device.id)

        for (deviceSurvey in deviceSurveys) {
            deviceSurveyController.deleteDeviceSurvey(deviceSurvey)
        }
        deviceRepository.deleteSuspending(device)
    }

    /**
     * Lists devices
     *
     * @param rangeStart first index
     * @param rangeEnd last index
     * @param status status
     * @return list of devices and count
     */
    suspend fun listDevices(rangeStart: Int?, rangeEnd: Int?, status: DeviceStatus?): Pair<List<DeviceEntity>, Long> {
        return deviceRepository.list(rangeStart, rangeEnd, status)
    }

    /**
     * Updates Devices status
     *
     * @param device device to update
     * @param status status
     */
    suspend fun updateDeviceStatus(device: DeviceEntity, status: DeviceStatus) {
        deviceRepository.updateDeviceStatus(device, status)
        deviceRepository.updateLastSeen(device, OffsetDateTime.now())
    }
}