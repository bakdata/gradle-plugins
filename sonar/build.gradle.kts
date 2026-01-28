plugins {
    kotlin("jvm")
    alias(libs.plugins.dokka)
}
apply(plugin = "org.gradle.kotlin.kotlin-dsl")

description = "Configures sonar for multi project setups for all jvm languages"

dependencies {
    implementation(libs.sonarqube)
}

tasks.withType<Test> {
    // overwrite the sonarqube env variable on travis
    environment("SONAR_SCANNER_HOME", "")
    environment("SONARQUBE_SCANNER_PARAMS", "{}")
}
