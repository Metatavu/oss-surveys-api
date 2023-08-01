package fi.metatavu.oss.api.impl.devices

import fi.metatavu.oss.api.metadata.DBMetadata
import fi.metatavu.oss.api.model.DeviceStatus
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.*

/**
 * Device JPA entity
 */
@Entity
@Table(name = "device")
class DeviceEntity: DBMetadata() {

    @Id
    lateinit var id: UUID

    @Column
    var name: String? = null

    @Column(nullable = false)
    lateinit var serialNumber: String

    @Column
    var description: String? = null

    @Column
    var location: String? = null

    @Column
    @Enumerated(EnumType.STRING)
    lateinit var deviceStatus: DeviceStatus

    @Column (nullable = false)
    lateinit var lastSeen: OffsetDateTime

    @Lob
    @Column
    lateinit var deviceKey: ByteArray

    override var creatorId: UUID? = null
    override var lastModifierId: UUID? = null

}