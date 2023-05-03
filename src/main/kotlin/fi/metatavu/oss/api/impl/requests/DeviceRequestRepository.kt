package fi.metatavu.oss.api.impl.requests

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import fi.metatavu.oss.api.model.DeviceApprovalStatus
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.util.UUID
import jakarta.enterprise.context.ApplicationScoped

/**
 * Repository class for device requests
 */
@ApplicationScoped
class DeviceRequestRepository: AbstractRepository<DeviceRequestEntity, UUID>() {

    /**
     * Creates a Device Request
     *
     * @param serialNumber serial number
     * @return uni with created device request
     */
    suspend fun create(serialNumber: String): DeviceRequestEntity {
        val deviceRequestEntity = DeviceRequestEntity()
        deviceRequestEntity.id = UUID.randomUUID()
        deviceRequestEntity.serialNumber = serialNumber
        deviceRequestEntity.approvalStatus = DeviceApprovalStatus.PENDING

        return persistSuspending(deviceRequestEntity)
    }

    /**
     * Finds a Device Request by serial number
     *
     * @param serialNumber serial number
     * @return uni with found device request
     */
    suspend fun findBySerialNumber(serialNumber: String): DeviceRequestEntity? {
        return find("serialNumber = ?1", serialNumber).firstResult<DeviceRequestEntity?>().awaitSuspending()
    }

    /**
     * Updates a Device Request
     *
     * @param deviceRequest device request
     * @return uni with updated device request
     */
    suspend fun update(deviceRequest: DeviceRequestEntity): DeviceRequestEntity {
        return persistSuspending(deviceRequest)
    }
}