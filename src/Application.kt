package com.example

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.webjars.*
import java.time.*
import io.ktor.features.*
import io.ktor.auth.*
import com.fasterxml.jackson.databind.*
import io.ktor.jackson.*
import io.ktor.util.KtorExperimentalAPI

@KtorExperimentalAPI
fun main(args: Array<String>) {
    io.ktor.server.jetty.EngineMain.main(args)
}

@KtorExperimentalAPI
fun Application.module() {
    install(Webjars) {
        path = "/webjars" //defaults to /webjars
        zone = ZoneId.systemDefault() //defaults to ZoneId.systemDefault()
    }

    install(Authentication) {
        basic("myBasicAuth") {
            realm = "Ktor Server"
            validate { if (it.name == "test" && it.password == "password") UserIdPrincipal(it.name) else null }
        }
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    DatabaseFactory.init()
    val urlService = UrlService()

    routing {
        get("/") {
            call.respondText("RUNNING", contentType = ContentType.Text.Plain)
        }

        // Static feature. Try to access `/static/ktor_logo.svg`
        static("/static") {
            resources("static")
        }

        get("/webjars") {
            call.respondText("<script src='/webjars/jquery/jquery.js'></script>", ContentType.Text.Html)
        }

        authenticate("myBasicAuth") {
            get("/protected/route/basic") {
                val principal = call.principal<UserIdPrincipal>()!!
                call.respondText("Hello ${principal.name}")
            }
        }

        get("/json/jackson") {
            call.respond(mapOf("hello" to "world"))
        }

        get("/urls") {
            val urls = urlService.getAllUrls()
            call.respond(HttpStatusCode.OK, urls)
        }

        get("/urls/{id}") {
            val id = call.parameters["id"]
                ?: throw IllegalStateException("Must provide id")

            val url = urlService.getUrl(id)

            if (url == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(HttpStatusCode.OK, url)
            }
        }

        post("/urls") {
            val url: InitUrl = call.receive()
            val createdUrl : Url = urlService.createShortUrl(url.fullUrl)

            call.respond(HttpStatusCode.Created, createdUrl)
        }
    }
}

data class InitUrl(val fullUrl: String)
