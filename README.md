[![Build Status](https://dev.azure.com/bakdata/public/_apis/build/status/bakdata.gradle-plugins?branchName=master)](https://dev.azure.com/bakdata/public/_build/latest?definitionId=3&branchName=master)
[![Sonarcloud status](https://sonarcloud.io/api/project_badges/measure?project=com.bakdata.gradle%3Agradle-plugins&metric=alert_status)](https://sonarcloud.io/dashboard?id=bakdata-com.bakdata.gradle%3Agradle-plugins)
[![Code coverage](https://sonarcloud.io/api/project_badges/measure?project=com.bakdata.gradle%3Agradle-plugins&metric=coverage)](https://sonarcloud.io/dashboard?id=bakdata-com.bakdata.gradle%3Agradle-plugins)

bakdata gradle plugins
======================

A collection of small Gradle plugins, mostly focused on deployment.

- **Sonar** Some defaults for easy integration of sonar on multi-module projects
- **Sonatype** is used for uploading to sonatype repos and ultimately publish to Maven Central
- **Release** adds configurable push behavior for version bumping
- **Jib** configures jib

[//]: # (TODO)

## Development

Snapshot versions of these plugins are published to Sonatype.
You can use them in your project by adding the following snippet to your `build.gradle.kts`

```
buildscript {
  repositories {
    maven {
      url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
  }
  dependencies {
    classpath("com.bakdata.gradle:sonar:0.0.1-SNAPSHOT")
    classpath("com.bakdata.gradle:sonatype:0.0.1-SNAPSHOT")
    classpath("com.bakdata.gradle:release:0.0.1-SNAPSHOT")
    classpath("com.bakdata.gradle:jib:0.0.1-SNAPSHOT")
  }
}

apply(plugin = "com.bakdata.sonar")
apply(plugin = "com.bakdata.sonatype")
apply(plugin = "com.bakdata.release")
apply(plugin = "com.bakdata.jib")
```
