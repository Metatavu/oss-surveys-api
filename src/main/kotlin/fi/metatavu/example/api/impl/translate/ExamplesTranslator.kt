package fi.metatavu.oss.api.impl.translate

import fi.metatavu.oss.api.persistence.model.Example
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import io.smallrye.mutiny.groups.UniAwait
import javax.enterprise.context.ApplicationScoped

/**
 * Translator class for Examples
 */
@ApplicationScoped
class ExamplesTranslator: AbstractTranslator<Example?, fi.metatavu.oss.api.model.Example?>() {

    override fun translate(entity: Example?): fi.metatavu.oss.api.model.Example {
        return fi.metatavu.oss.api.model.Example(
            id = entity!!.id,
            name = entity!!.name!!,
            amount = entity!!.amount!!
        )
    }

    /*override fun translate(entity: Uni<Example>?): fi.metatavu.oss.api.model.Example? {
        return entity?.onItem()?.transform { translate(it) }?.await()?.indefinitely()
    }*/

}