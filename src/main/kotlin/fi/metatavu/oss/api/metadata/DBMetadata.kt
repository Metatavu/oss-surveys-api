package fi.metatavu.oss.api.metadata

import java.time.OffsetDateTime
import java.util.*
import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate

/**
 * Class containing shared general properties of entities
 */
@MappedSuperclass
abstract class DBMetadata {

    @Column(nullable = false)
    open var createdAt: OffsetDateTime? = null

    @Column(nullable = false)
    open var modifiedAt: OffsetDateTime? = null

    abstract var creatorId: UUID?

    abstract var lastModifierId: UUID?

    @PrePersist
    fun onCreate() {
        createdAt = OffsetDateTime.now()
        modifiedAt = OffsetDateTime.now()
    }

    @PreUpdate
    fun onUpdate() {
        modifiedAt = OffsetDateTime.now()
    }

}