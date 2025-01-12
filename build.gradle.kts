plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google() // This is a shorthand for the Google Maven repository\
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.0") // Kotlin coroutines
    implementation("io.ktor:ktor-client-core:3.0.3") // Replace with the latest version
    implementation("io.ktor:ktor-client-cio:3.0.3")  // Or another engine like ktor-client-okhttp
    implementation("io.ktor:ktor-client-content-negotiation:3.0.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.3")
    implementation("io.ktor:ktor-client-logging-jvm:3.0.3")
    implementation("ch.qos.logback:logback-classic:1.5.13")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}