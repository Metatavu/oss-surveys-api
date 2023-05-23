package fi.metatavu.oss.api.impl.requests

import fi.metatavu.oss.api.metadata.DBMetadata
import fi.metatavu.oss.api.model.DeviceApprovalStatus
import java.util.*
import javax.persistence.*

/**
 * Device Request JPA entity
 */
@Entity
@Table(name = "devicerequest")
class DeviceRequestEntity: DBMetadata() {

    @Id
    lateinit var id: UUID

    @Column(nullable = false)
    lateinit var serialNumber: String

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    lateinit var approvalStatus: DeviceApprovalStatus

    @Column
    var name: String? = null

    @Column
    var description: String? = null

    @Column
    var location: String? = null

    override var creatorId: UUID? = null
    override var lastModifierId: UUID? = null
}