plugins {
    `kotlin-dsl`
    alias(libs.plugins.dokka)
}

description = "Configures Gradle Release plugin for usage in CI"

dependencies {
    implementation(libs.release)
}
