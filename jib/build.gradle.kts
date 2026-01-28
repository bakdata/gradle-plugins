plugins {
    kotlin("jvm")
    alias(libs.plugins.dokka)
}
apply(plugin = "org.gradle.kotlin.kotlin-dsl")

description = "Configures jib repository, tag and image name"

dependencies {
    implementation(libs.jib)
}
