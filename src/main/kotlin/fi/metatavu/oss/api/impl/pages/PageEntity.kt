package fi.metatavu.oss.api.impl.pages

import fi.metatavu.oss.api.impl.layouts.LayoutEntity
import fi.metatavu.oss.api.impl.surveys.SurveyEntity
import fi.metatavu.oss.api.metadata.DBMetadata
import javax.persistence.*
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

    @ManyToOne(optional = false)
    lateinit var survey: SurveyEntity

    @ManyToOne(optional = false)
    lateinit var layout: LayoutEntity

    @Column (nullable = false)
    var orderNumber: Int = 0

    override var creatorId: UUID? = null
    override var lastModifierId: UUID? = null
}