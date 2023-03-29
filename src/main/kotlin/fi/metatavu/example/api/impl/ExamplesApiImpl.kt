package fi.metatavu.example.api.impl

import fi.metatavu.oss.api.example.ExamplesController
import fi.metatavu.oss.api.impl.AbstractApi
import fi.metatavu.oss.api.impl.translate.ExamplesTranslator
import fi.metatavu.oss.api.model.Example
import fi.metatavu.oss.api.spec.ExamplesApi
import io.quarkus.hibernate.reactive.panache.Panache
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.util.*
import javax.annotation.security.RolesAllowed
import javax.enterprise.context.RequestScoped
import javax.inject.Inject
import javax.transaction.Transactional
import javax.ws.rs.core.Response

@RequestScoped
class ExamplesApiImpl: ExamplesApi, AbstractApi()  {

    @Inject
    lateinit var examplesController: ExamplesController

    @Inject
    lateinit var examplesTranslator: ExamplesTranslator

    /* EXAMPLES */
    override suspend fun listExamples(firstResult: Int?, maxResults: Int?): Response {

        loggedUserId ?: return createUnauthorized(NO_VALID_USER_MESSAGE)
        val examples = examplesController.list().awaitSuspending()

        return createOk(examplesTranslator.translate(examples))
        //return createOk(examples?.onItem()?.transform { examplesTranslator.translate(it) })
    }

    override suspend fun createExample(example: Example): Response {
        val userId = loggedUserId ?: return createUnauthorized(NO_VALID_USER_MESSAGE)
        val name = example.name
        val amount = example.amount

      //  val createdExample = Panache.withTransaction {
        val createdExample = examplesController.create(
                name = name,
                amount = amount,
                creatorId = userId
            ).awaitSuspending()

       // }.awaitSuspending()
       // return createOk(createdExample?.onItem()?.transform { examplesTranslator.translate(it)})
        return createOk(examplesTranslator.translate(createdExample))
    }

    override suspend fun findExample(exampleId: UUID): Response {
        loggedUserId ?: return createUnauthorized(NO_VALID_USER_MESSAGE)
        val foundExample = examplesController.findExample(exampleId).awaitSuspending() ?: return createNotFound("Example with ID $exampleId could not be found")
       // return createOk(foundExample.onItem().transform { examplesTranslator.translate(it) })
        return createOk(examplesTranslator.translate(foundExample))
    }


    override suspend fun updateExample(exampleId: UUID, example: Example): Response {
        val userId = loggedUserId ?: return createUnauthorized(NO_VALID_USER_MESSAGE)
        val name = example.name
        val amount = example.amount

        val exampleToUpdate = examplesController.findExample(exampleId).awaitSuspending() ?: return createNotFound("Example with ID $exampleId could not be found")

        return createOk(examplesTranslator.translate(exampleToUpdate))
        //return createOk(exampleToUpdate.onItem().transform { examplesTranslator.translate(it) })
    }

    override suspend fun deleteExample(exampleId: UUID): Response {
        loggedUserId ?: return createUnauthorized(NO_VALID_USER_MESSAGE)
        val foundExample = examplesController.findExample(exampleId).awaitSuspending() ?: return createNotFound("Example with ID $exampleId could not be found")
        //foundExample.onItem().invoke { it -> examplesController.deleteExample(it) }
        examplesController.deleteExample(foundExample).awaitSuspending()
        return createNoContent()
    }

    companion object {
        const val NO_VALID_USER_MESSAGE = "No valid user!"
    }

}
