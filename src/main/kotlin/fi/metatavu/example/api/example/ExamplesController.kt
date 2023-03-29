package fi.metatavu.oss.api.example

import fi.metatavu.oss.api.persistence.dao.ExampleRepository
import fi.metatavu.oss.api.persistence.model.Example
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Controller class for examples
 *
 * @author Jari Nykänen
 */
@ApplicationScoped
class ExamplesController {

    @Inject
    lateinit var exampleDAO: ExampleRepository

    /**
     * Creates a new Example
     *
     * @param name name
     * @param amount amount
     * @param creatorId Creator Id
     * @return created counter frame
     */
    @ReactiveTransactional
    fun create (name: String, amount: Int, creatorId: UUID): Uni<Example> {
        return exampleDAO.create(
            id = UUID.randomUUID(),
            name= name,
            amount = amount,
            creatorId = creatorId
        )
    }

    /**
     * Finds a example from the database
     *
     * @param exampleId example id to find
     * @return example or null if not found
     */
    @ReactiveTransactional
    fun findExample(exampleId: UUID): Uni<Example> {
        return exampleDAO.findById(exampleId)
    }

    /**
     * List examples
     *
     * @param firstResult First result. Defaults to 0 (optional)
     * @param maxResults Max results. Defaults to 10 (optional)
     * @return list of languages
     */
    fun list(): Uni<List<Example>> {
        return exampleDAO.listAll()
    }

    /**
     * Deletes a example from the database
     *
     * @param example example to delete
     */
    @ReactiveTransactional
    fun deleteExample(example: Example): Uni<Void> {
        return exampleDAO.delete(example)
    }
}