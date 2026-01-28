plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka-javadoc") version "2.1.0"
}
apply(plugin = "org.gradle.kotlin.kotlin-dsl")

description = "Sets up nexusPublish and closeAndReleaseRepository tasks to push to sonatype (and later synced to central)"

dependencies {
    implementation("io.github.gradle-nexus", "publish-plugin", "2.0.0")

    testImplementation("org.wiremock:wiremock:3.10.0")
    testImplementation("ru.lanwen.wiremock:wiremock-junit5:1.3.1")
}

tasks.withType<Test> {
    testLogging {
        showStandardStreams = true
    }
}
