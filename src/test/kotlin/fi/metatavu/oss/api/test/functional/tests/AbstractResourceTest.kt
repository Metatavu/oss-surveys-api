package fi.metatavu.oss.api.test.functional.tests

import fi.metatavu.oss.api.test.functional.resources.MqttResource
import io.quarkus.test.common.DevServicesContext
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import org.eclipse.microprofile.config.ConfigProvider
import org.junit.jupiter.api.AfterEach

/**
 * Abstract base class for resource tests
 */
@QuarkusTest
@QuarkusTestResource(MqttResource::class)
abstract class AbstractResourceTest {

    private var devServicesContext: DevServicesContext? = null

    /**
     * Devices are being created as a side effect from the device fetching its key for the first time,
     * therefore devices need special clean up as they cannot be handled as closeables.
     */
    @AfterEach
    fun cleanDevicesAfterEach() {
        createTestBuilder().use {
            it.manager.devices.list().forEach { device ->
                it.manager.devices.delete(device.id!!)
            }
        }
    }

    /**
     * Creates new test builder
     *
     * @return new test builder
     */
    protected fun createTestBuilder(): fi.metatavu.oss.api.test.functional.TestBuilder {
        return fi.metatavu.oss.api.test.functional.TestBuilder(getConfig())
    }

    /**
     * Returns config for tests.
     *
     * If tests are running in native mode, method returns config from devServicesContext and
     * when tests are running in JVM mode method returns config from the Quarkus config
     *
     * @return config for tests
     */
    private fun getConfig(): Map<String, String> {
        return getDevServiceConfig() ?: getQuarkusConfig()
    }

    /**
     * Returns test config from dev services
     *
     * @return test config from dev services
     */
    private fun getDevServiceConfig(): Map<String, String>? {
        return devServicesContext?.devServicesProperties()
    }

    /**
     * Returns test config from Quarkus
     *
     * @return test config from Quarkus
     */
    private fun getQuarkusConfig(): Map<String, String> {
        val config = ConfigProvider.getConfig()
        return config.propertyNames.associateWith { config.getConfigValue(it).rawValue }
    }

}