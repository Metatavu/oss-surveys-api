package fi.metatavu.oss.api.impl.pages.questions

import fi.metatavu.oss.api.impl.pages.PageEntity
import fi.metatavu.oss.api.model.PageQuestionType
import java.util.*
import javax.persistence.*

/**
 * JPA entity representing a question of a page
 */
@Entity
@Table(name = "pagequestion")
class PageQuestionEntity {

    @Id
    lateinit var id: UUID

    @OneToOne(optional = false)
    lateinit var page: PageEntity

    @Enumerated(EnumType.STRING)
    lateinit var type: PageQuestionType
}