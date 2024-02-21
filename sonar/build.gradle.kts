plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") version "0.9.17"
}
apply(plugin = "org.gradle.kotlin.kotlin-dsl")

description = "Configures sonar for multi project setups for all jvm languages"

dependencies {
    implementation( "org.sonarsource.scanner.gradle", "sonarqube-gradle-plugin", "4.4.1.3373")
}

tasks.withType<Test> {
    // overwrite the sonarqube env variable on travis
    environment("SONAR_SCANNER_HOME", "")
    environment("SONARQUBE_SCANNER_PARAMS", "{}")
}
