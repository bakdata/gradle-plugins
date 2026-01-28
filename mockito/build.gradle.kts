plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") version "2.0.0"
}
apply(plugin = "org.gradle.kotlin.kotlin-dsl")

description = "Configures Mockito as a Java agent for tests"
