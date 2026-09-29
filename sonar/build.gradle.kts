plugins {
    `kotlin-dsl`
    alias(libs.plugins.dokka)
}

description = "Configures sonar for multi project setups for all jvm languages"

dependencies {
    implementation(libs.sonarqube)
}

tasks.withType<Test> {
    // overwrite the sonarqube env variable on travis
    environment("SONAR_SCANNER_HOME", "")
    environment("SONARQUBE_SCANNER_PARAMS", "{}")
}
