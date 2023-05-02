package fi.metatavu.oss.api.impl.pages

import fi.metatavu.oss.api.impl.surveys.SurveyEntity
import fi.metatavu.oss.api.metadata.DBMetadata
import jakarta.persistence.*
import java.util.*

/**
 * JPA entity representing a page of a survey
 */
@Entity
@Table(name = "page")
class PageEntity : DBMetadata() {

    @Id
    lateinit var id: UUID

    @Column(nullable = false)
    lateinit var title: String

    @Column(nullable = false)
    lateinit var html: String

    @ManyToOne(optional = false)
    lateinit var survey: SurveyEntity

    override var creatorId: UUID? = null
    override var lastModifierId: UUID? = null
}