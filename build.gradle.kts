import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = "no.nav.syfo"
version = "1.0"

val ktorVersion = "3.4.0"
val prometheusVersion = "0.16.0"
val micrometerVersion = "1.16.3"
val slf4jVersion = "2.0.17"
val logbackVersion = "1.5.29"
val javaxVersion = "2.1.1"
val logstashEncoderVersion = "9.0"
val jacksonVersion = "2.21.0"
val jacksonDatabindVersion = "2.13.2.2"
val javaJwtVersion = "4.5.0"
val nimbusVersion = "10.7"
val kotestVersion = "6.1.3"
val kotestExtensionsVersion = "2.0.0"
val kotlinVersion = "2.3.10"
val mockkVersion = "1.14.9"
val postgresVersion = "42.7.9"
val hikariVersion = "7.0.2"
val flywayVersion = "12.0.1"
val gsonVersion = "2.13.2"
val kafkaVersion = "4.1.1"
val altinnKanalSchemasVersion = "2.0.0"
val avroVersion = "1.12.1"
val confluentVersion = "8.1.1"
val syfotjenesterVersion = "1.2020.07.02-07.44-62078cd74f7e"
val helseXmlVersion = "2.0.1"
val quartzSchedulerVersion = "2.5.2"
val kotestTestContainersExtensionVersion = "2.0.2"
val testcontainersVersion = "1.21.4"

val githubUser: String by project
val githubPassword: String by project

plugins {
    kotlin("jvm") version "2.2.21"
    id("org.jetbrains.kotlin.plugin.allopen") version "2.2.21"
    id("com.diffplug.spotless") version "8.2.1"
    id("com.gradleup.shadow") version "9.3.1"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
}

allOpen {
    annotation("no.nav.syfo.annotation.Mockable")
}

repositories {
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven/")
    maven(url = "https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
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
    implementation("io.ktor:ktor-client-apache5-jvm:$ktorVersion")

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
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")

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
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("io.kotest.extensions:kotest-assertions-ktor:$kotestExtensionsVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:$kotestTestContainersExtensionVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
}

configurations.implementation {
    exclude(group = "com.fasterxml.jackson.module", module = "jackson-module-scala_2.13")
}

kotlin {
    jvmToolchain(21)
}

java.toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    withType<ShadowJar> {
        filesMatching("META-INF/services/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
        mergeServiceFiles()
        isZip64 = true
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to "no.nav.syfo.AppKt",
                ),
            )
        }
    }
    withType<Test> {
        useJUnitPlatform()
    }
    named("check") {
        dependsOn("ktlintCheck")
    }
}
