[![Build and Publish](https://github.com/bakdata/gradle-plugins/actions/workflows/build-and-publish.yaml/badge.svg)](https://github.com/bakdata/gradle-plugins/actions/workflows/build-and-publish.yaml)
[![Sonarcloud status](https://sonarcloud.io/api/project_badges/measure?project=com.bakdata.gradle%3Agradle-plugins&metric=alert_status)](https://sonarcloud.io/dashboard?id=bakdata-com.bakdata.gradle%3Agradle-plugins)
[![Code coverage](https://sonarcloud.io/api/project_badges/measure?project=com.bakdata.gradle%3Agradle-plugins&metric=coverage)](https://sonarcloud.io/dashboard?id=bakdata-com.bakdata.gradle%3Agradle-plugins)

bakdata gradle plugins
======================

A collection of small Gradle plugins, mostly focused on deployment.

- **Sonar** Some defaults for easy integration of sonar on multi-module projects
- **Sonatype** is used for uploading to sonatype repos and ultimately publish to Maven Central
- **Release** adds configurable push behavior for version bumping
- **Jib** configures Jib repository, tag and image name
- **Mockito** configures Mockito as a Java agent for tests

## Development

Snapshot versions of these plugins are published to Sonatype.
You can use them in your project by adding the following snippet to your `settings.gradle.kts`

```
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven(url = "https://central.sonatype.com/repository/maven-snapshots")
    }
}
```
