package fi.metatavu.oss.api.impl.layouts

import fi.metatavu.oss.api.impl.AbstractApi
import fi.metatavu.oss.api.impl.UserRole
import fi.metatavu.oss.api.impl.pages.PagesController
import fi.metatavu.oss.api.model.Layout
import fi.metatavu.oss.api.spec.LayoutsApi
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import java.util.*
import javax.annotation.security.RolesAllowed
import javax.enterprise.context.RequestScoped
import javax.inject.Inject
import javax.ws.rs.core.Response

@RequestScoped
@OptIn(ExperimentalCoroutinesApi::class)
class LayoutApiImpl : LayoutsApi, AbstractApi() {

    @Inject
    lateinit var layoutController: LayoutController

    @Inject
    lateinit var layoutTranslator: LayoutTranslator

    @Inject
    lateinit var pagesController: PagesController

    @Inject
    lateinit var vertx: Vertx

    @ReactiveTransactional
    @RolesAllowed(UserRole.MANAGER.name)
    override fun listLayouts(firstResult: Int?, maxResults: Int?): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val (rangeStart, rangeEnd) = firstMaxToRange(firstResult, maxResults)
            val (layouts, count) = layoutController.list(rangeStart, rangeEnd)

            createOk(layoutTranslator.translate(layouts), count)
        }.asUni()

    @ReactiveTransactional
    @RolesAllowed(UserRole.MANAGER.name)
    override fun createLayout(layout: Layout): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        val created = layoutController.create(layout, userId)

        createOk(layoutTranslator.translate(created))
    }.asUni()

    @ReactiveTransactional
    @RolesAllowed(UserRole.MANAGER.name)
    override fun findLayout(layoutId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val found = layoutController.find(layoutId) ?: return@async createNotFoundWithMessage(LAYOUT, layoutId)
        createOk(layoutTranslator.translate(found))
    }.asUni()

    @ReactiveTransactional
    @RolesAllowed(UserRole.MANAGER.name)
    override fun updateLayout(layoutId: UUID, layout: Layout): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
            val found = layoutController.find(layoutId) ?: return@async createNotFoundWithMessage(LAYOUT, layoutId)
            val created = layoutController.update(found, layout, userId)

            createOk(layoutTranslator.translate(created))
        }.asUni()

    @ReactiveTransactional
    @RolesAllowed(UserRole.MANAGER.name)
    override fun deleteLayout(layoutId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val found = layoutController.find(layoutId) ?: return@async createNotFoundWithMessage(LAYOUT, layoutId)
        val (pages) = pagesController.listPages(found)
        if (pages.isNotEmpty()) {
            return@async createBadRequest("Cannot delete layout with pages")
        }

        layoutController.delete(found)

        createNoContent()
    }.asUni()
}