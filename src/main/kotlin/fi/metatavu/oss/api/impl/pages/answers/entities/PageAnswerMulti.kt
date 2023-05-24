package fi.metatavu.oss.api.impl.pages.answers.entities

import javax.persistence.Entity
import javax.persistence.Table

/**
 * Entity for the multi-option answer submitted for a survey page
 */
@Entity
@Table(name = "pageanswermulti")
class PageAnswerMulti : PageAnswerBaseEntity()