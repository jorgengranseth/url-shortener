package com.example

import io.ktor.http.*
import kotlin.test.*
import io.ktor.server.testing.*
import io.ktor.util.KtorExperimentalAPI

@KtorExperimentalAPI
class ApplicationTest {
    companion object {
        @JvmStatic
        fun setup() {
            println(" >>>>>>>>> Set database properties")
            System.setProperty("DATABASE_URL", "jdbc:postgresql://localhost:5432/ktor-starter")
            System.setProperty("DATABASE_USER", "test")
            System.setProperty("DATABASE_PASSWORD", "password")
            println(" <<<<<<<<< Set database properties")
        }
    }

    @Test
    fun testRoot() {
        setup()
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("HELLO WORLD!", response.content)
            }
        }
    }
}
