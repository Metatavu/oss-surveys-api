package fi.metatavu.oss.api.configuration

import org.eclipse.microprofile.config.ConfigProvider
import org.eclipse.microprofile.config.spi.ConfigSource
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Configurator for MQTT channel
 */
class MqttChannelConfigurator: ConfigSource {

    private val logger = org.slf4j.LoggerFactory.getLogger(MqttChannelConfigurator::class.java)
    private var activeUrl: URI? = null

    override fun getPropertyNames(): MutableSet<String> {
        return emptySet<String>().toMutableSet()
    }

    override fun getValue(propertyName: String?): String? {
        val result = when (propertyName) {
            "mp.messaging.connector.smallrye-mqtt.host" -> getActiveUrl().host
            "mp.messaging.connector.smallrye-mqtt.port" -> getActiveUrl().port.toString()
            "mp.messaging.connector.smallrye-mqtt.ssl" -> getActiveUrl().scheme.contains("ssl").toString()
            else -> null
        }

        if (result != null) {
            logger.debug("MQTT property $propertyName = $result")
        }

        return result
    }

    override fun getName(): String {
        return MqttChannelConfigurator::class.java.name
    }

    /**
     * Returns active MQTT server URL
     *
     * @return active MQTT server URL
     */
    private fun getActiveUrl(): URI {
        if (activeUrl != null) {
            return activeUrl!!
        }

        val urls = parseUrls()
        activeUrl = urls.first { isMqttServerAlive(it) }

        return activeUrl!!
    }

    /**
     * Tests if MQTT server is alive
     *
     * @param url MQTT server URL
     * @return true if MQTT server is alive; false otherwise
     */
    private fun isMqttServerAlive(url: URI): Boolean {
        logger.info("Testing MQTT server: $url")

        val port = url.port
        val host = url.host

        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), TimeUnit.SECONDS.toMillis(5).toInt())
                true
            }
        } catch (e: Exception) {
            println("Failed to connect to MQTT server at $host:$port - ${e.message}")
            false
        }
    }

    /**
     * Parses MQTT server URLs from configuration
     *
     * @return MQTT server URLs
     */
    private fun parseUrls(): List<URI> {
        val urls = ConfigProvider.getConfig().getValue("mqtt.urls", String::class.java)
        return urls.split(",").map { URI.create(it) }
    }

}