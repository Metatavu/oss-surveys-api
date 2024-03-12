package fi.metatavu.oss.api.medialibrary

import fi.metatavu.oss.api.impl.AbstractApi
import fi.metatavu.oss.api.spec.MediaLibraryApi
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import javax.enterprise.context.RequestScoped
import javax.inject.Inject
import javax.ws.rs.core.Response

/**
 * Api Implementation for Media Library
 */
@RequestScoped
@Suppress ("unused")
@OptIn(ExperimentalCoroutinesApi::class)
class MediaLibraryApiImpl: MediaLibraryApi, AbstractApi() {

    @Inject
    lateinit var vertx: Vertx

    @Inject
    lateinit var mediaLibraryController: MediaLibraryController

    override fun listMediaFiles(path: String): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val mediaFields = mediaLibraryController.listMediaFiles(path)
        createOk(mediaFields)
    }.asUni()
}