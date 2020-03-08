package com.example

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.config.HoconApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

@KtorExperimentalAPI
object DatabaseFactory {
    fun init() {
        val isHeroku = System.getenv("IS_HEROKU")?.equals("true") ?: false
        val dbConfig = if (isHeroku) herokuConfig() else appConfig()

        Database.connect(hikari(dbConfig))

        transaction {
            val flyway = Flyway.configure()
                .dataSource(dbConfig.url, dbConfig.user, dbConfig.password)
                .load()
            flyway.migrate()
        }
    }

    private fun hikari(dbConfig: DBConfig): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "org.postgresql.Driver"
        config.jdbcUrl = dbConfig.url
        config.username = dbConfig.user
        config.password = dbConfig.password
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }

    suspend fun <T> dbQuery(block: () -> T): T =
        withContext(Dispatchers.IO) {
            transaction { block() }
        }

}

data class DBConfig(
    val user: String,
    val password: String,
    val url: String
)

fun herokuConfig(): DBConfig {
    val dbUri = URI(System.getenv("DATABASE_URL"))
    val userInfo = dbUri.userInfo.split(":")

    return DBConfig(
        user = userInfo[0],
        password = userInfo[1],
        url = "jdbc:postgresql://${dbUri.host}:${dbUri.port}${dbUri.path}"
    )
}

@KtorExperimentalAPI
fun appConfig(): DBConfig {
    val hoconConfig = HoconApplicationConfig(ConfigFactory.load())

    return DBConfig(
        user = hoconConfig.property("db.dbUser").getString(),
        password = hoconConfig.property("db.dbPassword").getString(),
        url = hoconConfig.property("db.jdbcUrl").getString()
    )
}
