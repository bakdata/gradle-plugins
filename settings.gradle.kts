pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        maven(url = "https://central.sonatype.com/repository/maven-snapshots")
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
