pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "gradle-plugins"

include(
    "sonar",
    "sonatype",
    "release",
    "jib",
    "mockito",
)
