package fi.metatavu.example.api.test.functional.tests

import fi.metatavu.example.api.test.functional.tests.ExamplesResourceTest
import fi.metatavu.oss.api.test.functional.resources.LocalTestProfile
import io.quarkus.test.junit.QuarkusIntegrationTest
import io.quarkus.test.junit.TestProfile

/**
 * Native tests for example resources
 *
 * @author Antti Leppä
 */
@QuarkusIntegrationTest
@TestProfile(LocalTestProfile::class)
class NativeExamplesResourceTest: ExamplesResourceTest() {

}
