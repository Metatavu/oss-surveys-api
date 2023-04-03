package fi.metatavu.oss.api.impl.surveys

import fi.metatavu.oss.api.metadata.DBMetadata
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "survey")
class SurveyEntity : DBMetadata() {

    @Id
    lateinit var id: UUID

    @Column(nullable = false)
    lateinit var title: String

    override var creatorId: UUID? = null
    override var lastModifierId: UUID? = null
}