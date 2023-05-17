package fi.metatavu.oss.api.impl.layouts

import fi.metatavu.oss.api.model.LayoutVariableType
import java.util.*
import javax.persistence.*

/**
 * JPA entity representing a layout variable
 */
@Entity
@Table(name = "layoutvariable")
class LayoutVariableEntity {

    @Id
    lateinit var id: UUID

    @ManyToOne(optional = false)
    lateinit var layout: LayoutEntity

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    lateinit var variabletype: LayoutVariableType

    @Column(nullable = false)
    lateinit var variablekey: String
}