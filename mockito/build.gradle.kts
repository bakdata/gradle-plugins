plugins {
    kotlin("jvm")
    alias(libs.plugins.dokka)
}
apply(plugin = "org.gradle.kotlin.kotlin-dsl")

description = "Configures Mockito as a Java agent for tests"
