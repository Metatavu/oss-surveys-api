package fi.metatavu.oss.api.impl.pages.questions

import org.apache.commons.lang3.ObjectUtils.Null
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.Table

/**
 * Possible answer option to a question
 */
@Entity
@Table(name = "questionoption")
class QuestionOptionEntity {

    @Id
    lateinit var id: UUID

    @ManyToOne(optional = false)
    lateinit var question: PageQuestionEntity

    @Column(nullable = false)
    lateinit var value: String

    @Column(nullable = false)
    var orderNumber: Int? = null
}