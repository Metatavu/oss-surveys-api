package fi.metatavu.oss.api.impl.devices

import fi.metatavu.oss.api.impl.devicesurveys.DeviceSurveyController
import fi.metatavu.oss.api.impl.requests.DeviceRequestEntity
import fi.metatavu.oss.api.model.DeviceStatus
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.security.PublicKey
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Controller class for devices (transactions should start here)
 */
@ApplicationScoped
class DeviceController {

    @Inject
    lateinit var deviceRepository: DeviceRepository

    @Inject
    lateinit var deviceSurveyController: DeviceSurveyController

    /**
     * Creates a device
     *
     * @param deviceRequest device request
     * @param deviceKey device key
     * @param userId user id
     * @return uni with created device
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
        newDevice.creatorId = userId
        newDevice.lastModifierId = userId

        return deviceRepository.create(device = newDevice)
    }

    /**
     * Gets device private key by device id
     *
     * @param id id
     * @return device private key base 64 encoded
     */
    suspend fun getDeviceKey(id: UUID): ByteArray {
        return deviceRepository.findById(id).awaitSuspending().deviceKey
    }

    /**
     * Finds a Device
     *
     * @param id id
     * @return uni with found device
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
        val (deviceSurveys) = deviceSurveyController.listDeviceSurveysByDevice(device.id)

        for (deviceSurvey in deviceSurveys) {
            deviceSurveyController.deleteDeviceSurvey(deviceSurvey)
        }
        deviceRepository.deleteSuspending(device)
    }

    /**
     * Lists devices
     *
     * @param firstResult first result
     * @param maxResults max results
     * @return list of devices and count
     */
    suspend fun listDevices(firstResult: Int?, maxResults: Int?): Pair<List<DeviceEntity>, Long> {
        return deviceRepository.listAllWithPaging(
            page = firstResult,
            pageSize = maxResults
        )
    }
}