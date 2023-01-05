plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka") version "1.7.20"
}

description = "Configures sonar for multi project setups for all jvm languages"

dependencies {
    implementation( "org.sonarsource.scanner.gradle", "sonarqube-gradle-plugin", "3.4.0.2513")
}

tasks.withType<Test> {
    // overwrite the sonarqube env variable on travis
    environment("SONAR_SCANNER_HOME", "")
    environment("SONARQUBE_SCANNER_PARAMS", "{}")
}
