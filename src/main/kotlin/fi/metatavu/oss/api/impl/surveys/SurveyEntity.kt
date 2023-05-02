package fi.metatavu.oss.api.impl.surveys

import fi.metatavu.oss.api.metadata.DBMetadata
import fi.metatavu.oss.api.model.SurveyStatus
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "survey")
class SurveyEntity : DBMetadata() {

    @Id
    lateinit var id: UUID

    @Column(nullable = false)
    lateinit var title: String

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    lateinit var status: SurveyStatus

    override var creatorId: UUID? = null
    override var lastModifierId: UUID? = null
}