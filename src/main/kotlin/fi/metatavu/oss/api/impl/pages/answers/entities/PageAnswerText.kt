package fi.metatavu.oss.api.impl.pages.answers.entities

import javax.persistence.Column
import javax.persistence.Entity

/**
 * Entity for the text answer submitted for a survey page
 */
@Entity
@javax.persistence.Table(name = "pageanswertext")
class PageAnswerText : PageAnswerBaseEntity() {

    @Column
    lateinit var text: String
}