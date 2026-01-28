plugins {
    kotlin("jvm")
    alias(libs.plugins.dokka)
}
apply(plugin = "org.gradle.kotlin.kotlin-dsl")

description = "Configures Gradle Release plugin for usage in CI"

dependencies {
    implementation(libs.release)
}
