package fi.metatavu.oss.api.test.functional.tests

import fi.metatavu.oss.api.test.functional.resources.LocalTestProfile
import fi.metatavu.oss.api.test.functional.resources.MqttResource
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusIntegrationTest
import io.quarkus.test.junit.TestProfile

@QuarkusIntegrationTest
@QuarkusTestResource(MqttResource::class)
@TestProfile(LocalTestProfile::class)
class NativeDeviceSurveysTestIT: DeviceSurveysTestIT() {
}