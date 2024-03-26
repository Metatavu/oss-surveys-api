package fi.metatavu.oss.api.test.functional.tests

import fi.metatavu.oss.api.test.functional.resources.AwsResource
import fi.metatavu.oss.api.test.functional.resources.LocalTestProfile
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusIntegrationTest
import io.quarkus.test.junit.TestProfile

@QuarkusIntegrationTest
@QuarkusTestResource(AwsResource::class)
@TestProfile(LocalTestProfile::class)
class NativeMediaLibraryTestIT : MediaLibraryTestIT() {
}