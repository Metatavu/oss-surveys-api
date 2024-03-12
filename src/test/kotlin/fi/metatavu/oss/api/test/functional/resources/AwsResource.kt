package fi.metatavu.oss.api.test.functional.resources

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.InputStream


var localstackImage: DockerImageName = DockerImageName.parse("localstack/localstack:latest")

/**
 * Test resource for S3 storage
 */
class AwsResource : QuarkusTestResourceLifecycleManager {
    override fun start(): Map<String, String> {
        s3.start()
        val config: MutableMap<String, String> = HashMap()

        val bucketName = "testbucket"
        val endpoint = s3.getEndpointOverride(LocalStackContainer.Service.S3)

        config["bucket.name"] = bucketName
        config["quarkus.s3.aws.region"] = s3.region
        config["quarkus.s3.aws.credentials.static-provider.access-key-id"] = s3.accessKey
        config["quarkus.s3.aws.credentials.static-provider.secret-access-key"] = s3.secretKey
        config["quarkus.s3.endpoint-override"] = "http://${endpoint.host}:${endpoint.port}"
        config["quarkus.s3.aws.credentials.type"] = "static"

        // Create initial client
        val s3Client = S3Client.builder()
            .region(Region.US_WEST_2)
            .endpointOverride(endpoint)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        s3.accessKey,
                        s3.secretKey
                    )
                )
            ).build()
        setupBucket(s3Client, bucketName)
        return config
    }

    /**
     * Sets up the bucket with some initial data
     *
     * @param s3Client S3 client
     * @param bucketName name of the bucket
     */
    private fun setupBucket(
        s3Client: S3Client,
        bucketName: String,
    ) {
        s3Client.createBucket(
            CreateBucketRequest.builder()
                .bucket(bucketName)
                .build()
        )
        val file1 = "file1.txt"
        val file2 = "file2.txt"

        loadResource(file1).use {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(file1)
                    .build(), RequestBody.fromInputStream(it, it!!.available().toLong())
            )
        }
        loadResource(file2).use {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key("subfolder/"+file2)
                    .build(), RequestBody.fromInputStream(it, it!!.available().toLong())
            )
        }
    }

    /**
     * Loads a resource from the classpath
     *
     * @param name name of the resource
     * @return input stream of the resource
     */
    private fun loadResource(name: String): InputStream? {
        return this.javaClass.classLoader.getResourceAsStream("aws/$name")
    }

    override fun stop() {
        s3.stop()
    }

    companion object {
        var s3: LocalStackContainer = LocalStackContainer(localstackImage)
            .withServices(LocalStackContainer.Service.S3)
    }
}