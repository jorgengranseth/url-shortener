package com.example

import io.ktor.util.KtorExperimentalAPI
import org.jetbrains.exposed.sql.*
import java.security.MessageDigest
import java.util.*

val SHORT_LENGTH = 8

private object UrlTable: Table() {
    val full_url: Column<String> = varchar("full_url", 255)
    val short_url: Column<String> = varchar("short_url", SHORT_LENGTH)
    val clicks: Column<Int> = integer("clicks")
    override val primaryKey = PrimaryKey(short_url, name = "PK_URL_ID")
}

data class Url(
    val fullUrl: String,
    val shortUrl: String,
    val clicks: Int
)

val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
val encoder: Base64.Encoder = Base64.getEncoder()

fun hashAndBase64Encode(s : String): String =
    s.toByteArray()
        .let { digest.update(it) }
        .let { digest.digest() }
        .let { encoder.encodeToString(it) }
        .take(SHORT_LENGTH)

@KtorExperimentalAPI
class UrlService {
    suspend fun getAllUrls(): List<Url> = DatabaseFactory.dbQuery {
        UrlTable.selectAll().map { toUrl(it) }
    }

    suspend fun createShortUrl(url: String): Url {
        var key = ""
        DatabaseFactory.dbQuery {
            val insert = UrlTable.insert {
                it[full_url] = url
                it[short_url] = hashAndBase64Encode(url)
            }

            key = insert get UrlTable.short_url
        }

        return getUrl(key)!!
    }

    suspend fun getUrl(id: String): Url? = DatabaseFactory.dbQuery {
        UrlTable.select { (UrlTable.short_url eq id) }
            .mapNotNull { toUrl(it) }
            .singleOrNull()
    }

    private fun toUrl(row: ResultRow): Url =
        Url(
            fullUrl = row[UrlTable.full_url],
            shortUrl = row[UrlTable.short_url],
            clicks = row[UrlTable.clicks]
        )

}
