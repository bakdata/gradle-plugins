dependencies {
    implementation( "io.codearte.gradle.nexus", "gradle-nexus-staging-plugin", "0.12.0")
    implementation("de.marcphilipp.gradle", "nexus-publish-plugin", "0.1.1")
    testImplementation("org.junit.jupiter", "junit-jupiter-params", "5.3.0")
    testImplementation("com.github.tomakehurst:wiremock:2.20.0")
    testImplementation("ru.lanwen.wiremock:wiremock-junit5:1.1.1")
}