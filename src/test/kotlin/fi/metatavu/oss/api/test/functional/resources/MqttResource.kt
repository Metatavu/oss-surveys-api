package fi.metatavu.oss.api.test.functional.resources

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import org.slf4j.event.Level
import org.testcontainers.hivemq.HiveMQContainer
import org.testcontainers.utility.DockerImageName

/**
 * HiveMQ MQTT Test Resource
 */
class MqttResource: QuarkusTestResourceLifecycleManager {

    override fun start(): MutableMap<String, String> {
        val config = HashMap<String, String>()
        hivemqContainer.start()
        config["mqtt.server.url"] = hivemqContainer.host + ":" + hivemqContainer.mqttPort
        config["mp.messaging.outgoing.surveys.host"] = hivemqContainer.host
        config["mp.messaging.outgoing.surveys.port"] = hivemqContainer.mqttPort.toString()
        config["mp.messaging.incoming.status.host"] = hivemqContainer.host
        config["mp.messaging.incoming.status.port"] = hivemqContainer.mqttPort.toString()
        config["mp.messaging.incoming.status.topic"] = "test/test/+/status"

        return config
    }

    override fun stop() {
        hivemqContainer.stop()
    }

    companion object {
        val hivemqContainer = HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce")).withLogLevel(Level.DEBUG)
    }
}