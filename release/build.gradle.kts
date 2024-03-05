plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") version "1.9.10"
}
apply(plugin = "org.gradle.kotlin.kotlin-dsl")

description = "Configures Gradle Release plugin for usage in CI"

dependencies {
    implementation("net.researchgate", "gradle-release", "3.0.2")
    implementation("gradle.plugin.org.hildan.gradle", "gradle-github-changelog", "1.12.1")
}
