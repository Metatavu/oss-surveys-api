package fi.metatavu.oss.api.medialibrary

import fi.metatavu.oss.api.model.MediaFile
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import org.eclipse.microprofile.config.inject.ConfigProperty
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.ListObjectsRequest
import software.amazon.awssdk.services.s3.model.ListObjectsResponse
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

    /**
     * Lists media files in a given path
     *
     * @param path path to list
     * @return list of media files
     */
    suspend fun listMediaFiles(path: String): List<MediaFile> {
        val listRequest: ListObjectsRequest = ListObjectsRequest.builder().bucket(bucketName)
            .prefix(path)
            .build()

        val resp = Uni.createFrom().completionStage(s3Client.listObjects(listRequest))
            .onFailure().recoverWithItem(
                ListObjectsResponse.builder().contents(emptyList()).build()
            )
            .awaitSuspending()

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