plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") version "1.9.10"
}
apply(plugin = "org.gradle.kotlin.kotlin-dsl")

description = "Configures jib" //TODO

dependencies {
    implementation("com.google.cloud.tools.jib", "com.google.cloud.tools.jib.gradle.plugin", "3.4.4")
}
