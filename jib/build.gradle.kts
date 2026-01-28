plugins {
    kotlin("jvm")
}
apply(plugin = "org.gradle.kotlin.kotlin-dsl")

description = "Configures jib repository, tag and image name"

dependencies {
    implementation("com.google.cloud.tools.jib", "com.google.cloud.tools.jib.gradle.plugin", "3.4.4")
}
