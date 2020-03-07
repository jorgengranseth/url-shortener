package com.example

import com.example.DatabaseFactory.dbQuery
import com.example.User.name
import com.typesafe.config.ConfigFactory
import io.ktor.config.HoconApplicationConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

@KtorExperimentalAPI
object DatabaseFactory {

    private val appConfig = HoconApplicationConfig(ConfigFactory.load())
    private val dbUrl = appConfig.property("db.jdbcUrl").getString()
    private val dbUser = appConfig.property("db.dbUser").getString()
    private val dbPassword = appConfig.property("db.dbPassword").getString()

    fun init() {
        Database.connect(hikari())

        transaction {
            val flyway = Flyway.configure().dataSource(dbUrl, dbUser, dbPassword).load()
            flyway.migrate()
        }
    }

    private fun hikari(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "org.postgresql.Driver"
        config.jdbcUrl = dbUrl
        config.username = dbUser
        config.password = dbPassword
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

@KtorExperimentalAPI
class UserService {
    suspend fun getAllUsers(): List<UserInst> = dbQuery {
        User.selectAll().map { toUser(it) }
    }

    suspend fun createUser(user: UserInst): UserInst {
        var key = 0
        dbQuery {
            val insert = User.insert {
                it[name] = user.name
            }

            key = insert get User.id
        }

        return getUser(key)!!
    }

    suspend fun getUser(id: Int): UserInst? = dbQuery {
        User.select { (User.id eq id) }
            .mapNotNull { toUser(it) }
            .singleOrNull()
    }

    private fun toUser(row: ResultRow): UserInst =
        UserInst(
            id = row[User.id],
            name = row[User.name]
        )

}

private object User: Table() {
    val id: Column<Int> = integer("id").autoIncrement()
    val name: Column<String> = varchar("name", 20)
    override val primaryKey = PrimaryKey(id, name = "PD_User_ID")
}

data class UserInst(
    val id: Int?,
    val name: String
)

