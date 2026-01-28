plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") version "2.1.0"
}
apply(plugin = "org.gradle.kotlin.kotlin-dsl")

description = "Configures Gradle Release plugin for usage in CI"

dependencies {
    implementation("net.researchgate", "gradle-release", "3.1.0")
}
