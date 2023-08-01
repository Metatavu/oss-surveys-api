package fi.metatavu.oss.api.impl.devices

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import fi.metatavu.oss.api.model.DeviceStatus
import io.quarkus.panache.common.Sort
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.time.OffsetDateTime
import java.util.UUID
import javax.enterprise.context.ApplicationScoped

/**
 * Repository class for devices
 */
@ApplicationScoped
class DeviceRepository: AbstractRepository<DeviceEntity, UUID>() {

    /**
     * Lists devices
     *
     * @param rangeStart range start
     * @param rangeEnd range end
     * @param status optional status filter
     * @return list of devices and count
     */
    suspend fun list(rangeStart: Int?, rangeEnd: Int?, status: DeviceStatus?): Pair<List<DeviceEntity>, Long> {
        val sort = Sort.descending("modifiedAt")
        val query = if (status == null) {
            findAll(sort)
        } else {
            find("deviceStatus", sort, status)
        }

        return applyRangeToQuery(
                query = query,
                firstIndex = rangeStart,
                lastIndex = rangeEnd
        )
    }

    /**
     * Creates a Device
     *
     * @param device device
     * @return created device
     */
    suspend fun create(device: DeviceEntity): DeviceEntity {
        return persistSuspending(device)
    }

    /**
     * Updates a Device
     *
     * @param device device to update
     * @param name name
     * @param description description
     * @param location location
     * @param lastModifierId last modifier id
     * @return updated device
     */
    suspend fun update(
        device: DeviceEntity,
        name: String,
        description: String?,
        location: String?,
        lastModifierId: UUID,
    ): DeviceEntity {
        device.name = name
        device.description = description
        device.location = location
        device.lastModifierId = lastModifierId

        return persistSuspending(device)
    }

    /**
     * Updates Devices status
     *
     * @param device device to update
     * @param status status
     */
    suspend fun updateDeviceStatus(device: DeviceEntity, status: DeviceStatus) {
        device.deviceStatus = status
        persistSuspending(device)
    }

    /**
     * Updates Devices last seen
     *
     * @param device device to update
     * @param lastSeen last seen
     */
    suspend fun updateLastSeen(device: DeviceEntity, lastSeen: OffsetDateTime) {
        device.lastSeen = lastSeen
        persistSuspending(device)
    }

    /**
     * Finds a Device a by serial number
     *
     * @param serialNumber serial number
     * @return found device
     */
    suspend fun findBySerialNumber(serialNumber: String): DeviceEntity? {
        return find("serialNumber", serialNumber).firstResult<DeviceEntity>().awaitSuspending()
    }
}