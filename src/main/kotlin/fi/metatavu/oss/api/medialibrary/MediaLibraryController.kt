package fi.metatavu.oss.api.medialibrary

import fi.metatavu.oss.api.model.MediaFile
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.Logger
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.ListObjectsRequest
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import kotlin.io.path.Path

/**
 * Controller for media library
 */
@ApplicationScoped
class MediaLibraryController {

    @Inject
    lateinit var s3Client: S3AsyncClient

    @ConfigProperty(name = "bucket.name")
    lateinit var bucketName: String

    @Inject
    lateinit var logger: Logger

    /**
     * Lists media files in a given path
     *
     * @param path path to list
     * @return list of media files
     */
    suspend fun listMediaFiles(path: String): List<MediaFile>? {
        val listRequest: ListObjectsRequest = ListObjectsRequest.builder().bucket(bucketName)
            .prefix(path)
            .build()

        val resp = try {
             Uni.createFrom().completionStage(s3Client.listObjects(listRequest)).awaitSuspending()
        } catch (e: Exception) {
            logger.error(e.message)
            return null
        }

        return if (resp.hasContents()) {
            resp.contents()
                .map {
                    MediaFile(
                        name = it.key().substringAfterLast("/"),
                        path = Path(it.key()).normalize().toString()
                    )
                }
        } else {
            emptyList()
        }
    }

}