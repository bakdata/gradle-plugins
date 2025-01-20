plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") version "2.0.0"
}
apply(plugin = "org.gradle.kotlin.kotlin-dsl")

description = "Configures Gradle Release plugin for usage in CI"

dependencies {
    implementation("net.researchgate", "gradle-release", "3.1.0")
    implementation("org.hildan.gradle", "gradle-github-changelog", "2.2.0")
}
