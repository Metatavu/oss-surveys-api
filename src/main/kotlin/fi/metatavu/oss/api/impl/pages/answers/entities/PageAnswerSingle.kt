package fi.metatavu.oss.api.impl.pages.answers.entities

import fi.metatavu.oss.api.impl.pages.questions.QuestionOptionEntity
import javax.persistence.Entity
import javax.persistence.OneToOne

/**
 * Entity for the single-option answer submitted for a survey page
 */
@Entity
@javax.persistence.Table(name = "pageanswersingle")
class PageAnswerSingle : PageAnswerBaseEntity() {

    @OneToOne(optional = false)
    lateinit var option: QuestionOptionEntity
}