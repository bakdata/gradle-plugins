plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") version "1.9.10"
}
apply(plugin = "org.gradle.kotlin.kotlin-dsl")

description = "Sets up nexusPublish and closeAndReleaseRepository tasks to push to sonatype (and later synced to central)"

dependencies {
    implementation("au.com.console:kassava:1.0.0")
    implementation("io.github.gradle-nexus", "publish-plugin", "1.3.0")

    testImplementation("org.junit.jupiter", "junit-jupiter-params", "5.3.0")
    testImplementation("com.github.tomakehurst:wiremock:2.20.0")
    testImplementation("ru.lanwen.wiremock:wiremock-junit5:1.1.1")
}

tasks.withType<Test> {
    testLogging {
        showStandardStreams = true
    }
}
