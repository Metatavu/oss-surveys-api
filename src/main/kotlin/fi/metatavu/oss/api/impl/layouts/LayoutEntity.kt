package fi.metatavu.oss.api.impl.layouts

import fi.metatavu.oss.api.metadata.DBMetadata
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

/**
 * page layout entity
 */
@Entity
@Table(name = "layout")
class LayoutEntity : DBMetadata() {

    @Id
    lateinit var id: UUID

    @Column(nullable = false)
    lateinit var name: String

    @Column(nullable = false)
    lateinit var thumbnailUrl: String

    @Column(nullable = false)
    lateinit var html: String

    override var creatorId: UUID? = null
    override var lastModifierId: UUID? = null
}