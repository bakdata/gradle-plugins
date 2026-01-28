pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
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
