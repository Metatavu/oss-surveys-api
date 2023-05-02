package fi.metatavu.plastep.api.migrations

import io.quarkus.runtime.StartupEvent
import io.quarkus.runtime.util.ExceptionUtil
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.exception.LiquibaseException
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.resource.ResourceAccessor
import org.eclipse.microprofile.config.inject.ConfigProperty
import jakarta.enterprise.event.Observes

/**
 * Custom liquibase migrator class
 *
 * @author Jari Nykänen
 */
class LiquibaseMigrator {

    @ConfigProperty(name = "custom.liquibase.migrate")
    var runMigration = false

    @ConfigProperty(name = "quarkus.datasource.jdbc.url")
    var datasourceUrl: String? = null

    @ConfigProperty(name = "quarkus.datasource.username")
    var datasourceUsername: String? = null

    @ConfigProperty(name = "quarkus.datasource.password")
    var datasourcePassword: String? = null

    @ConfigProperty(name = "quarkus.liquibase.change-log")
    var changeLogLocation: String? = null

    /**
     * Runs liquibase migration
     *
     * @throws LiquibaseException liquibase exception
     */
    @Throws(LiquibaseException::class)
    fun runLiquibaseMigration(@Observes event: StartupEvent?) {
        if (runMigration) {
            var liquibase: Liquibase? = null
            try {
                val resourceAccessor: ResourceAccessor =
                    ClassLoaderResourceAccessor(Thread.currentThread().contextClassLoader)
                val conn = DatabaseFactory.getInstance()
                    .openConnection(
                        datasourceUrl,
                        datasourceUsername,
                        datasourcePassword,
                        null,
                        resourceAccessor
                    )
                liquibase = Liquibase(changeLogLocation, resourceAccessor, conn)
                liquibase.update(Contexts(), LabelExpression())
            } catch (e: Exception) {
                println("Liquibase Migration Exception: " + ExceptionUtil.generateStackTrace(e))
            } finally {
                liquibase?.close()
            }
        }
    }
}