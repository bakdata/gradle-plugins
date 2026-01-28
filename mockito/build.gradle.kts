plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka-javadoc") version "2.1.0"
}
apply(plugin = "org.gradle.kotlin.kotlin-dsl")

description = "Configures Mockito as a Java agent for tests"
