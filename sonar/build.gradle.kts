plugins {
    kotlin("jvm") version "1.4.20"
    id("org.jetbrains.dokka") version "1.9.10"
}

description = "Configures sonar for multi project setups for all jvm languages"

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    implementation( "org.sonarsource.scanner.gradle", "sonarqube-gradle-plugin", "4.4.1.3373")
}

tasks.withType<Test> {
    // overwrite the sonarqube env variable on travis
    environment("SONAR_SCANNER_HOME", "")
    environment("SONARQUBE_SCANNER_PARAMS", "{}")
}
