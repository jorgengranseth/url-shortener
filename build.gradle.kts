import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val exposed_version: String by project
val hikari_version: String by project
val postgres_version: String by project
val flyway_version: String by project

plugins {
    application
    kotlin("jvm") version "1.3.61"
    id("com.github.johnrengelman.shadow") version "5.0.0"
    id("org.flywaydb.flyway") version "5.2.4"
}

group = "com.example"
version = "0.0.1"

application {
    mainClassName = "io.ktor.server.jetty.EngineMain"
}

repositories {
    mavenLocal()
    jcenter()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
}

tasks.withType<Jar> {
    manifest {
        attributes(
            mapOf(
                "Main-Class" to application.mainClassName
            )
        )
    }
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()
}

tasks {
    "run"(JavaExec::class) {
        environment("DATABASE_URL", "jdbc:postgresql://localhost:5432/ktor-starter")
        environment("DATABASE_USER", "test")
        environment("DATABASE_PASSWORD", "password")
    }
    "runShadow"(JavaExec::class) {
        environment("DATABASE_URL", "jdbc:postgresql://localhost:5432/ktor-starter")
        environment("DATABASE_USER", "test")
        environment("DATABASE_PASSWORD", "password")
    }
    "test"(Test::class) {
        environment("DATABASE_URL", "jdbc:postgresql://localhost:5432/ktor-starter")
        environment("DATABASE_USER", "test")
        environment("DATABASE_PASSWORD", "password")
    }
}

flyway {
    url = System.getenv("DATABASE_URL")
    user = System.getenv("DATABASE_USER")
    password = System.getenv("DATABASE_PASSWORD")
    baselineOnMigrate=true
    locations = arrayOf("filesystem:resources/db/migration")
}

dependencies {
    implementation("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", kotlin_version)
    implementation("io.ktor", "ktor-server-jetty", ktor_version)
    implementation("ch.qos.logback", "logback-classic", logback_version)
    implementation("io.ktor", "ktor-server-core", ktor_version)
    implementation("io.ktor", "ktor-server-host-common", ktor_version)
    implementation("io.ktor", "ktor-webjars", ktor_version)
    implementation("io.ktor", "ktor-auth", ktor_version)
    implementation("io.ktor", "ktor-jackson", ktor_version)
    implementation("org.jetbrains.exposed", "exposed-core", exposed_version)
    implementation("org.jetbrains.exposed", "exposed-dao", exposed_version)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposed_version)
    implementation("com.zaxxer", "HikariCP", hikari_version)
    implementation("org.postgresql", "postgresql", postgres_version)
    implementation("org.flywaydb", "flyway-core", flyway_version)
    testImplementation("io.ktor", "ktor-server-tests", ktor_version)
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")
