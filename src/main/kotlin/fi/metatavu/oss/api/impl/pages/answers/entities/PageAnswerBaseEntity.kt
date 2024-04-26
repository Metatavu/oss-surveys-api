package fi.metatavu.oss.api.impl.pages.answers.entities

import fi.metatavu.oss.api.impl.devices.DeviceEntity
import fi.metatavu.oss.api.impl.pages.PageEntity
import fi.metatavu.oss.api.metadata.DBMetadata
import java.util.*
import javax.persistence.*

/**
 * Entity for the answer submitted for a survey page
 */
@Entity
@Table(name = "pageanswer")
@Inheritance(strategy = InheritanceType.JOINED)
class PageAnswerBaseEntity : DBMetadata() {

    @Id
    lateinit var id: UUID

    @ManyToOne(optional = false)
    lateinit var page: PageEntity

    @ManyToOne
    var device: DeviceEntity? = null

    @Column(unique = true)
    var answerKey: String? = null

    override var creatorId: UUID? = null
    override var lastModifierId: UUID? = null
}
