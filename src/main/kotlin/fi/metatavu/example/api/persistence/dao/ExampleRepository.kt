package fi.metatavu.oss.api.persistence.dao

import fi.metatavu.oss.api.persistence.model.Example
import fi.metatavu.example.api.persistence.dao.AbstractRepository
import io.smallrye.mutiny.Uni
import java.util.*
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class ExampleRepository: AbstractRepository<Example, UUID>() {
    fun create(id: UUID?, name: String, amount: Int, creatorId: UUID): Uni<Example> {
        val example = Example()
        example.id = id
        example.name = name
        example.amount = amount
        example.creatorId = creatorId
        return persist(example)
    }
}