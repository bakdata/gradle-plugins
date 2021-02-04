plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") version "0.9.17"
}
apply(plugin = "org.gradle.kotlin.kotlin-dsl")

description = "Sets up nexusPublish and closeAndReleaseRepository tasks to push to sonatype (and later synced to central)"

dependencies {
    implementation("au.com.console:kassava:1.0.0")
    implementation("io.codearte.gradle.nexus", "gradle-nexus-staging-plugin", "0.22.0")
    implementation("de.marcphilipp.gradle", "nexus-publish-plugin", "0.4.0")

    testImplementation("org.junit.jupiter", "junit-jupiter-params", "5.3.0")
    testImplementation("com.github.tomakehurst:wiremock:2.20.0")
    testImplementation("ru.lanwen.wiremock:wiremock-junit5:1.1.1")
}

tasks.withType<Test> {
    testLogging {
        showStandardStreams = true
    }
}

