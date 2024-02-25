package ktor.starter.data.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.config.*
import kotlinx.coroutines.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.*
import javax.sql.DataSource

/**
 * Makes a connection to a Postgres database.
 *
 * In order to connect to your running Postgres process,
 * please specify the following parameters in your configuration file:
 * - postgres.url -- Url of your running database process.
 * - postgres.user -- Username for database connection
 * - postgres.password -- Password for database connection
 *
 * If you don't have a database process running yet, you may need to [download]((https://www.postgresql.org/download/))
 * and install Postgres and follow the instructions [here](https://postgresapp.com/).
 * Then, you would be able to edit your url,  which is usually "jdbc:postgresql://host:port/database", as well as
 * user and password values.
 *
 *
 * @param embedded -- if [true] defaults to an embedded database for tests that runs locally in the same process.
 * In this case you don't have to provide any parameters in configuration file, and you don't have to run a process.
 *
 * @return [Connection] that represent connection to the database. Please, don't forget to close this connection when
 * your application shuts down by calling [Connection.close]
 * */
fun Application.connectToPostgres(): DataSource {
    Class.forName("org.postgresql.Driver")
    environment.config.validatePostgresConfig()
    val (url, user, password) = environment.config.retrievePostgresConfig()
    val dataSource = hikari(url, user, password)

    val f = Flyway.configure().dataSource(dataSource).load()
    f.migrate()

    Database.connect(dataSource)

    // TODO - separate this out and make usable for all table definitions.
    transaction {
        val creators = SchemaUtils.statementsRequiredToActualizeScheme()
        if (creators.isNotEmpty()) {
            val output = creators.fold("\n") { acc, c ->
                "$acc\n$c"
            }
            throw Exception("""
Must add flyway migration statements: ${output}
            
If you continue to see this error, ensure that your file naming is correct.
https://documentation.red-gate.com/fd/migrations-184127470.html
        
            """)
        }
    }

    return dataSource
}

object PostgresConfigs {
    const val PostgresUrl = "postgres.url"
    const val PostgresUser = "postgres.user"
    const val PostgresPassword = "postgres.password"

    fun allRequiredConfigs() : List<String> = listOf(
        PostgresUrl,
        PostgresUser,
        PostgresPassword
    )
}

class PostgresConfigurationException(missingProperty: String) : Exception("""
    
    ** MISSING APPLICATION CONFIGURATION PROPERTY **
    The application configuration is found to be lacking `${missingProperty}`.
    Please ensure this is added correctly to the application.(yml|json|properties)
    file or the runtime environment.
""".trimIndent())

private fun ApplicationConfig.validatePostgresConfig() {
    PostgresConfigs.allRequiredConfigs().forEach {
        propertyOrNull(it) ?: throw PostgresConfigurationException(it)
    }
}

private fun ApplicationConfig.retrievePostgresConfig() : List<String> {
    return listOf(
        property(PostgresConfigs.PostgresUrl).getString(),
        property(PostgresConfigs.PostgresUser).getString(),
        property(PostgresConfigs.PostgresPassword).getString()
    )
}

fun hikari(url: String, user: String, password: String) : DataSource {
    return HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        jdbcUrl = url
        username = user
        this.password = password
        maximumPoolSize = 3
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }.let(::HikariDataSource)
}
