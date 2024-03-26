package fi.metatavu.oss.api.test.functional.tests

import fi.metatavu.oss.api.test.functional.resources.AwsResource
import fi.metatavu.oss.api.test.functional.resources.LocalTestProfile
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Test class for testing MediaLibrary API
 */
@QuarkusTest
@TestProfile(LocalTestProfile::class)
@QuarkusTestResource(AwsResource::class)
class MediaLibraryTestIT: AbstractResourceTest() {

    /**
     * Tests listing S3 files with different prefixes
     */
    @Test
    fun testMediaLibrary() = createTestBuilder().use { testBuilder ->
        val mediaFiles = testBuilder.manager.mediaLibrary.list("")
        assertTrue(mediaFiles.isNotEmpty())
        assertEquals(3, mediaFiles.size)

        val mediaFiles1 = testBuilder.manager.mediaLibrary.list("invalid")
        assertTrue(mediaFiles1.isEmpty())

        val mediaFiles2 = testBuilder.manager.mediaLibrary.list("subfolder")
        assertTrue(mediaFiles2.isNotEmpty())
        assertEquals(1, mediaFiles2.size)
        assertEquals("file2.txt", mediaFiles2[0].name)
        assertEquals("subfolder/file2.txt", mediaFiles2[0].path)

        val mediaFiles3 = testBuilder.manager.mediaLibrary.list("special-characters")
        assertTrue(mediaFiles3.isNotEmpty())
        assertEquals(1, mediaFiles3.size)
        assertEquals("fileäöå.txt", mediaFiles3[0].name)
        assertEquals("special-characters/fileäöå.txt", mediaFiles3[0].path)

        testBuilder.consumer.mediaLibrary.assertListFail(403)
        testBuilder.empty.mediaLibrary.assertListFail(401)
        testBuilder.notvalid.mediaLibrary.assertListFail(401)
    }
}