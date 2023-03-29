package fi.metatavu.oss.api.persistence.model

import java.time.OffsetDateTime
import java.util.*
import javax.persistence.*

/**
 * Entity class for Example
 *
 * @author Jari Nykänen
 */
@Entity
@Table(name = "example")
class Example {
    @Id
    var id: UUID? = null

    @Column(nullable = false)
    var name: String? = null

    @Column(nullable = false)
    var amount: Int? = null

    @Column(nullable = false)
    var createdAt: OffsetDateTime? = null

    @Column(nullable = false)
    var modifiedAt: OffsetDateTime? = null

    @Column
    var creatorId: UUID? = null

    @Column
    var lastModifierId: UUID? = null

    /**
     * JPA pre-persist event handler
     */
    @PrePersist
    fun onCreate() {
        createdAt = OffsetDateTime.now()
        modifiedAt = OffsetDateTime.now()
    }

    /**
     * JPA pre-update event handler
     */
    @PreUpdate
    fun onUpdate() {
        modifiedAt = OffsetDateTime.now()
    }
}