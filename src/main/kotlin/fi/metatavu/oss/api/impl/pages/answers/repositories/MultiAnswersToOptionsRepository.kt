package fi.metatavu.oss.api.impl.pages.answers.repositories

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import fi.metatavu.oss.api.impl.pages.answers.entities.MultiAnswersToOptions
import fi.metatavu.oss.api.impl.pages.answers.entities.PageAnswerMulti
import fi.metatavu.oss.api.impl.pages.questions.QuestionOptionEntity
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.util.*
import javax.enterprise.context.ApplicationScoped

/**
 * Repository for MultiAnswersToOptions entity, made to manage many-to-many relations
 */
@ApplicationScoped
class MultiAnswersToOptionsRepository : AbstractRepository<MultiAnswersToOptions, UUID>() {

    /**
     * Creates new MultiAnswersToOptions entity
     *
     * @param id id
     * @param pageAnswerMulti page answer multi
     * @param option question option
     * @return created MultiAnswersToOptions entity
     */
    suspend fun create(
        id: UUID,
        pageAnswerMulti: PageAnswerMulti,
        option: QuestionOptionEntity
    ): MultiAnswersToOptions {
        val multiAnswersToOptions = MultiAnswersToOptions()
        multiAnswersToOptions.id = id
        multiAnswersToOptions.pageAnswerMulti = pageAnswerMulti
        multiAnswersToOptions.questionOption = option
        return persistSuspending(multiAnswersToOptions)
    }

    /**
     * Lists MultiAnswersToOptions entities by page answer
     *
     * @param pageAnswer page answer
     * @return list of MultiAnswersToOptions entities
     */
    suspend fun listByPageAnswer(pageAnswer: PageAnswerMulti): List<MultiAnswersToOptions> {
        return list("pageAnswerMulti", pageAnswer).awaitSuspending()
    }
}