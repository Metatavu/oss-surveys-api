package fi.metatavu.oss.api.impl.pages.questions

import fi.metatavu.oss.api.impl.abstracts.AbstractRepository
import io.quarkus.panache.common.Sort
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.util.UUID
import javax.enterprise.context.ApplicationScoped

/**
 * Repository for Question Options
 */
@ApplicationScoped
class QuestionOptionRepository: AbstractRepository<QuestionOptionEntity, UUID>() {

    /**
     * Creates answer option for the question
     *
     * @param id id
     * @param question question that the option belongs to
     * @param value answer option text
     * @param orderNumber order number
     * @return created question option
     */
    suspend fun create(
        id: UUID,
        question: PageQuestionEntity,
        value: String,
        orderNumber: Int
    ): QuestionOptionEntity {
        val questionOptionEntity = QuestionOptionEntity()
        questionOptionEntity.id = id
        questionOptionEntity.question = question
        questionOptionEntity.value = value
        questionOptionEntity.orderNumber = orderNumber
        return persistSuspending(questionOptionEntity)
    }

    /**
     * Lists answer options by question
     *
     * @param question
     * @return list of answer options ordered by order number
     */
    suspend fun listByQuestion(question: PageQuestionEntity): List<QuestionOptionEntity> {
        return list("question", Sort.ascending("orderNumber"), question).awaitSuspending()
    }
}