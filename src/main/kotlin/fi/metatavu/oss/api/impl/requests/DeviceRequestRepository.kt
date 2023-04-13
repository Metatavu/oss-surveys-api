package fi.metatavu.oss.api.impl.requests

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import fi.metatavu.oss.api.model.DeviceApprovalStatus
import io.smallrye.mutiny.Uni
import java.util.UUID
import javax.enterprise.context.ApplicationScoped

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
    fun create(serialNumber: String): Uni<DeviceRequestEntity> {
        val deviceRequestEntity = DeviceRequestEntity()
        deviceRequestEntity.id = UUID.randomUUID()
        deviceRequestEntity.serialNumber = serialNumber
        deviceRequestEntity.approvalStatus = DeviceApprovalStatus.PENDING

        return persist(deviceRequestEntity)
    }

    /**
     * Finds a Device Request by serial number
     *
     * @param serialNumber serial number
     * @return uni with found device request
     */
    fun findBySerialNumber(serialNumber: String): Uni<DeviceRequestEntity?> {
        return find("serialnumber = ?1", serialNumber).firstResult()
    }

    /**
     * Updates a Device Request
     *
     * @param deviceRequest device request
     * @return uni with updated device request
     */
    fun update(deviceRequest: DeviceRequestEntity): Uni<DeviceRequestEntity> {
        return persist(deviceRequest)
    }
}