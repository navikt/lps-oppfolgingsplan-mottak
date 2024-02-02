import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = "no.nav.syfo"
version = "1.0"

val ktorVersion = "2.3.8"
val prometheusVersion = "0.16.0"
val micrometerVersion = "1.12.2"
val slf4jVersion = "1.7.36"
val logbackVersion = "1.4.14"
val javaxVersion = "2.1.1"
val logstashEncoderVersion = "7.4"
val jacksonVersion = "2.16.1"
val jacksonDatabindVersion = "2.13.2.2"
val javaJwtVersion = "4.4.0"
val nimbusVersion = "9.37.3"
val detektVersion = "1.23.0"
val kotestVersion = "5.8.0"
val kotestExtensionsVersion = "2.0.0"
val kotlinVersion = "1.9.22"
val mockkVersion = "1.13.9"
val postgresVersion = "42.7.1"
val postgresEmbeddedVersion = "0.13.3"
val hikariVersion = "5.1.0"
val flywayVersion = "7.5.2"
val gsonVersion = "2.10.1"
val kafkaVersion = "3.6.1"
val altinnKanalSchemasVersion = "2.0.0"
val avroVersion = "1.11.3"
val confluentVersion = "7.5.3"
val syfotjenesterVersion = "1.2020.07.02-07.44-62078cd74f7e"
val helseXmlVersion = "1.0.4"
val quartzSchedulerVersion = "2.3.2"

val githubUser: String by project
val githubPassword: String by project

plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.22"
    id("com.diffplug.spotless") version "6.25.0"
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.5"
}

allOpen {
    annotation("no.nav.syfo.annotation.Mockable")
}

repositories {
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven/")
    maven {
        url = uri("https://maven.pkg.github.com/navikt/tjenestespesifikasjoner")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
    maven {
        url = uri("https://maven.pkg.github.com/navikt/syfotjenester")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.scala-lang"
            && requested.name == "scala-library"
            && (requested.version == "2.13.3")
        ) {
            useVersion("2.13.9")
            because("fixes critical bug CVE-2022-36944 in 2.13.6")
        }
    }
}

dependencies {
    // Ktor server
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-apache-jvm:$ktorVersion")

    // Auth
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("com.auth0:java-jwt:$javaJwtVersion")
    implementation("com.nimbusds:nimbus-jose-jwt:$nimbusVersion")

    // API
    implementation("javax.ws.rs:javax.ws.rs-api:$javaxVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")

    // Database
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")

    // Logging
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    // Metrics and Prometheus
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_pushgateway:$prometheusVersion")

    // JSON/XML parsing
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:$jacksonVersion")

    // Kafka
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("org.apache.kafka:kafka_2.13:$kafkaVersion") {
        exclude(group = "log4j")
    }
    implementation("no.nav.altinnkanal.avro:altinnkanal-schemas:$altinnKanalSchemasVersion")
    implementation("org.apache.avro:avro:$avroVersion")
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion") {
        exclude(group = "log4j", module = "log4j")
    }
    implementation("no.nav.syfotjenester:oppfolgingsplanlps:$syfotjenesterVersion")
    implementation("no.nav.helse.xml:oppfolgingsplan:$helseXmlVersion")

    // Scheduling
    implementation("org.quartz-scheduler:quartz:$quartzSchedulerVersion")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("io.kotest.extensions:kotest-assertions-ktor:$kotestExtensionsVersion")
    testImplementation("io.mockk:mockk:${mockkVersion}")
    testImplementation("com.opentable.components:otj-pg-embedded:$postgresEmbeddedVersion")
}

detekt {
    toolVersion = detektVersion
    config.setFrom(file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}

configurations.implementation {
    exclude(group = "com.fasterxml.jackson.module", module = "jackson-module-scala_2.13")
}

kotlin {
    jvmToolchain(19)
}

java.toolchain {
    languageVersion.set(JavaLanguageVersion.of(19))
}

tasks {
    withType<ShadowJar> {
        manifest.attributes["Main-Class"] = "no.nav.syfo.AppKt"
    }
    withType<Test> {
        useJUnitPlatform()
    }
}

