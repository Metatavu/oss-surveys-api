package fi.metatavu.oss.api.test.functional.tests

import fi.metatavu.oss.api.test.functional.resources.LocalTestProfile
import io.quarkus.test.junit.QuarkusIntegrationTest
import io.quarkus.test.junit.TestProfile

@QuarkusIntegrationTest
@TestProfile(LocalTestProfile::class)
class NativeLayoutTestIT: LayoutTestIT() {
}