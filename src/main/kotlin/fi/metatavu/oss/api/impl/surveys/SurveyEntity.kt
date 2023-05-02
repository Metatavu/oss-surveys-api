package fi.metatavu.oss.api.impl.surveys

import fi.metatavu.oss.api.metadata.DBMetadata
import fi.metatavu.oss.api.model.SurveyStatus
import java.util.*
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

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