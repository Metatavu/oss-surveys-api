package fi.metatavu.example.api.test.functional.tests

import fi.metatavu.oss.api.test.functional.resources.LocalTestProfile
import fi.metatavu.oss.test.client.models.Example
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for Examples
 *
 * @author Jari Nykänen
 * @author Antti Leppä
 */
@QuarkusTest
@TestProfile(LocalTestProfile::class)
class ExamplesResourceTest: AbstractResourceTest() {

    /**
     * Tests list Examples
     */
    @Test
    fun listExamples() {
        createTestBuilder().use {
            /*val emptyList = it.manager.examples.listExamples(
                firstResult = null,
                maxResults = null
            )

            assertEquals(0, emptyList.size)*/

            it.manager.examples.createDefaultExample()
            it.manager.examples.createDefaultExample()

            val listWithTwoEntries = it.consumer.examples.listExamples(
                firstResult = null,
                maxResults = null
            )

            assertEquals(2, listWithTwoEntries.size)

        }
    }

    /**
     * Tests creating Example
     */
    @Test
    fun createExample() {
        createTestBuilder().use {
            val createdExample = it.manager.examples.createDefaultExample()
            assertNotNull(createdExample)
            assertNotNull(createdExample.name)
            assertNotNull(createdExample.amount)
        }
    }

    /**
     * Tests finding Example
     */
    @Test
    fun findExample() {
        createTestBuilder().use {
            val createdExample = it.manager.examples.createDefaultExample()
            assertNotNull(createdExample)

            val foundExample = it.manager.examples.findExample(createdExample.id!!)
            assertNotNull(foundExample)
        }
    }



    /**
     * Tests deleting Example
     */
    @Test
    fun deleteExample() {
        createTestBuilder().use {
            val createdExample = it.manager.examples.createDefaultExample()
            assertNotNull(createdExample)

            it.manager.examples.deleteExample(createdExample.id!!)

            val emptyListAfterDelete = it.manager.examples.listExamples(
                firstResult = null,
                maxResults = null
            )

            assertEquals(0, emptyListAfterDelete.size)
        }
    }
}