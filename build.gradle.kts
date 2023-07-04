import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = "no.nav.syfo"
version = "1.0"

val ktorVersion = "2.3.2"
val prometheusVersion = "0.15.0"
val micrometerVersion = "1.8.4"
val slf4jVersion = "1.7.36"
val logbackVersion = "1.2.11"
val javaxVersion = "2.1.1"
val logstashEncoderVersion = "7.0.1"
val jacksonVersion = "2.13.2"
val jacksonDatabindVersion = "2.13.2.2"
val javaJwtVersion = "4.4.0"
val nimbusVersion = "9.31"
val detektVersion = "1.23.0"
val kotestVersion = "5.6.2"
val kotestExtensionsVersion = "2.0.0"
val kotlinVersion = "1.8.22"
val mockkVersion = "1.13.5"

val githubUser: String by project
val githubPassword: String by project

plugins {
    kotlin("jvm") version "1.8.22"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.8.21"
    id("com.diffplug.gradle.spotless") version "3.18.0"
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.0"
}

allOpen {
    annotation("no.nav.syfo.annotation.Mockable")
}

repositories {
    mavenCentral()
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


    // Auth
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("com.auth0:java-jwt:$javaJwtVersion")
    implementation("com.nimbusds:nimbus-jose-jwt:$nimbusVersion")

    // API
    implementation("javax.ws.rs:javax.ws.rs-api:$javaxVersion")

    // Logging
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    // Metrics and Prometheus
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_pushgateway:$prometheusVersion")

    // JSON parsing
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("io.kotest.extensions:kotest-assertions-ktor:$kotestExtensionsVersion")
    testImplementation("io.mockk:mockk:${mockkVersion}")

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
    jvmToolchain(17)
}

java.toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
    withType<ShadowJar> {
        manifest.attributes["Main-Class"] = "no.nav.syfo.StartApplicationKt"
    }
    withType<Test> {
        useJUnitPlatform()
    }
}

