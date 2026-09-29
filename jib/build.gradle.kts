plugins {
    `kotlin-dsl`
    alias(libs.plugins.dokka)
}

description = "Configures jib repository, tag and image name"

dependencies {
    implementation(libs.jib)
}
