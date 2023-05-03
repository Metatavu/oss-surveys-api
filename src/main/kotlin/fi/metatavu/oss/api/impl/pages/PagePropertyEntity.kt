package fi.metatavu.oss.api.impl.pages

import fi.metatavu.oss.api.model.PagePropertyType
import javax.persistence.*
import java.util.*

/**
 * JPA entity representing a property of a survey page
 */
@Entity
@Table(name = "pageproperty")
class PagePropertyEntity {

    @Id
    lateinit var id: UUID

    @Column(nullable = false)
    lateinit var propertyKey: String

    @Column(nullable = false)
    lateinit var value: String

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    lateinit var type: PagePropertyType

    @ManyToOne(optional = false)
    lateinit var page: PageEntity
}