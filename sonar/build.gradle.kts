plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") version "2.1.0"
}
apply(plugin = "org.gradle.kotlin.kotlin-dsl")

description = "Configures sonar for multi project setups for all jvm languages"

dependencies {
    implementation("org.sonarsource.scanner.gradle", "sonarqube-gradle-plugin", "6.3.1.5724")
}

tasks.withType<Test> {
    // overwrite the sonarqube env variable on travis
    environment("SONAR_SCANNER_HOME", "")
    environment("SONARQUBE_SCANNER_PARAMS", "{}")
}
