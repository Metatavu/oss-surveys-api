package fi.metatavu.oss.api.impl.pages

import java.util.*
import javax.persistence.*

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

    @ManyToOne(optional = false)
    lateinit var page: PageEntity
}