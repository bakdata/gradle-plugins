plugins {
    `kotlin-dsl`
    alias(libs.plugins.dokka)
}

description = "Sets up nexusPublish and closeAndReleaseRepository tasks to push to sonatype (and later synced to central)"

dependencies {
    implementation(libs.nexus.publish)

    testImplementation(libs.wiremock)
    testImplementation(libs.wiremock.junit)
}

tasks.withType<Test> {
    testLogging {
        showStandardStreams = true
    }
}
