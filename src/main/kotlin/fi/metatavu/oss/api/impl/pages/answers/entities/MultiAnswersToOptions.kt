package fi.metatavu.oss.api.impl.pages.answers.entities

import fi.metatavu.oss.api.impl.pages.questions.QuestionOptionEntity
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.Table

/**
 * Connection needed for storing answers to questions with multiple answers
 */
@Entity
@Table(name = "multianswerstooptions")
class MultiAnswersToOptions {

    @Id
    lateinit var id: UUID

    @ManyToOne(optional = false)
    lateinit var pageAnswerMulti: PageAnswerMulti

    @ManyToOne(optional = false)
    lateinit var questionOption: QuestionOptionEntity
}