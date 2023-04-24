package fi.metatavu.oss.api.impl.devicesurveys

import fi.metatavu.oss.api.impl.devices.DeviceEntity
import fi.metatavu.oss.api.impl.surveys.SurveyEntity
import fi.metatavu.oss.api.metadata.DBMetadata
import fi.metatavu.oss.api.model.DeviceSurveyStatus
import java.time.OffsetDateTime
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "devicesurvey")
class DeviceSurveyEntity: DBMetadata() {

    @Id
    lateinit var id: UUID

    @ManyToOne
    lateinit var survey: SurveyEntity

    @ManyToOne
    lateinit var device: DeviceEntity

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    lateinit var status: DeviceSurveyStatus

    @Column
    var publishStartTime: OffsetDateTime? = null

    @Column
    var publishEndTime: OffsetDateTime? = null

    override var creatorId: UUID? = null
    override var lastModifierId: UUID? = null
}