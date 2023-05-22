package fi.metatavu.oss.api.test.functional

import fi.metatavu.jaxrs.test.functional.builder.AbstractAccessTokenTestBuilder
import fi.metatavu.jaxrs.test.functional.builder.AbstractTestBuilder
import fi.metatavu.jaxrs.test.functional.builder.auth.*
import fi.metatavu.oss.api.test.functional.auth.TestBuilderAuthentication
import fi.metatavu.oss.test.client.infrastructure.ApiClient
import java.io.IOException
import java.net.URL
import kotlin.jvm.Throws

/**
 * Abstract test builder class
 *
 * @author Jari Nykänen
 * @author Antti Leppä
 */
class TestBuilder(private val config: Map<String, String>): AbstractAccessTokenTestBuilder<ApiClient>() {

    val manager: TestBuilderAuthentication = createManager()
    val consumer: TestBuilderAuthentication = createConsumer()
    val empty: TestBuilderAuthentication = TestBuilderAuthentication(this, NullAccessTokenProvider())
    val notvalid: TestBuilderAuthentication = TestBuilderAuthentication(this, InvalidAccessTokenProvider())

    override fun createTestBuilderAuthentication(
        abstractTestBuilder: AbstractTestBuilder<ApiClient, AccessTokenProvider>,
        authProvider: AccessTokenProvider
    ): AuthorizedTestBuilderAuthentication<ApiClient, AccessTokenProvider> {
        return TestBuilderAuthentication(this, authProvider)
    }

    /**
     * Returns authentication resource authenticated as manager
     */
    @Throws(IOException::class)
    private fun createManager(): TestBuilderAuthentication {
        val authServerUrl = config.getValue("quarkus.oidc.auth-server-url").substringBeforeLast("/").substringBeforeLast("/")
        val realm = getKeycloakRealm()
        val clientId = "ui"
        val username = "manager"
        val password = "test"
        return TestBuilderAuthentication(this, KeycloakAccessTokenProvider(authServerUrl, realm, clientId, username, password, null))
    }

    /**
     * Returns authentication resource authenticated as consumer
     */
    @Throws(IOException::class)
    private fun createConsumer(): TestBuilderAuthentication {
        val authServerUrl = config.getValue("quarkus.oidc.auth-server-url").substringBeforeLast("/").substringBeforeLast("/")
        val realm = getKeycloakRealm()
        val clientId = "ui"
        val username = "consumer"
        val password = "test"
        return TestBuilderAuthentication(this, KeycloakAccessTokenProvider(authServerUrl, realm, clientId, username, password, null))
    }

    private fun getKeycloakRealm(): String {
        val serverUrl = URL(config["quarkus.oidc.auth-server-url"]!!)
        val pattern = Regex("(/realms/)([a-z]*)")
        val match = pattern.find(serverUrl.path)!!
        val (_, realm) = match.destructured
        return realm
    }
}