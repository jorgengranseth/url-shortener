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
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.jackson.*
import io.ktor.util.KtorExperimentalAPI
import org.slf4j.LoggerFactory

@KtorExperimentalAPI
fun main(args: Array<String>) {
    io.ktor.server.jetty.EngineMain.main(args)
}

@KtorExperimentalAPI
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
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

    if (!testing) DatabaseFactory.init()
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
            try {
                val createdUrl : Url = urlService.createShortUrl(url.fullUrl)
                call.respond(HttpStatusCode.Created, createdUrl)
            } catch (err: IllegalStateException) {
                call.respond(HttpStatusCode.Conflict)
            }

        }

        delete("/urls") {
            try {
                val url: InitUrl = call.receive()
                val deletes = urlService.deleteUrl(url.fullUrl)

                if (deletes == 0) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(HttpStatusCode.OK)
                }
            } catch (err: MissingKotlinParameterException) {
                call.respond(HttpStatusCode.BadRequest)
            }
        }

        val redirectLogger = LoggerFactory.getLogger("Redirect")
        get("/{id}") {
            val id = call.parameters["id"]
            id?.let { urlService.getUrl(it) }
                ?.also { redirectLogger.info("Redirecting $id to ${it.fullUrl}") }
                ?.let { call.respondRedirect(it.fullUrl, permanent = false) }
                ?: call.respond(HttpStatusCode.NotFound)

        }
    }
}

data class InitUrl(val fullUrl: String)
