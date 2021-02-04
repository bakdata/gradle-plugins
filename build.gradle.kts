buildscript {
    dependencies {
        classpath("org.gradle.kotlin:plugins:1.2.0")
        classpath("com.gradle.publish:plugin-publish-plugin:0.10.1")
    }
}

plugins {
    // release
    id("net.researchgate.release") version "2.6.0"
    // eat your own dog food - apply the plugins to this plugin project
    id("com.bakdata.sonar") version "1.1.4"
    id("com.bakdata.sonatype") version "1.1.4"
    id("org.hildan.github.changelog") version "0.8.0"
}

allprojects {
    // required for local self-publish
    plugins.apply("maven-publish")

    repositories {
        maven(url = "https://plugins.gradle.org/m2/")
    }

    group = "com.bakdata.gradle"

    tasks.withType<Test> {
        maxParallelForks = 4
    }
}

configure<com.bakdata.gradle.SonatypeSettings> {
    developers {
        developer {
            name.set("Arvid Heise")
            id.set("AHeise")
        }
    }
}

configure<org.hildan.github.changelog.plugin.GitHubChangelogExtension> {
    githubUser = "bakdata"
    futureVersionTag = findProperty("changelog.releaseVersion")?.toString()
    sinceTag = findProperty("changelog.sinceTag")?.toString()
}

subprojects {
    apply(plugin = "java")

    dependencies {
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.3.0")
        "testImplementation"("org.junit.jupiter:junit-jupiter-api:5.3.0")
        "testImplementation"("org.assertj", "assertj-core", "3.11.1")
        "testImplementation"("org.junit-pioneer", "junit-pioneer", "0.3.0")
    }
}

// config for gradle plugin portal
subprojects.forEach { project ->
    with(project) {
        // com.gradle.plugin-publish depends on java-gradle-plugin, but it screws a bit this project
        apply(plugin = "java-gradle-plugin")
        apply(plugin = "com.gradle.plugin-publish")
        // java-gradle-plugin requires this block, but we already added the definitions in META-INF for unit testing...
        configure<GradlePluginDevelopmentExtension> {
            plugins {
                create("${project.name.capitalize()}Plugin") {
                    id = "com.bakdata.${project.name}"
                    implementationClass = "com.bakdata.gradle.${project.name.capitalize()}Plugin"
                    description = project.description
                }
            }
        }
        // actual block of plugin portal config, need to be done on each subproject as the plugin does not support multi-module projects yet...
        configure<com.gradle.publish.PluginBundleExtension> {
            website = "https://github.com/bakdata/gradle-plugins"
            vcsUrl = "https://github.com/bakdata/gradle-plugins"
            (plugins) {
                "${name.capitalize()}Plugin" {
                    displayName = "Bakdata $name plugin"
                    tags = listOf("bakdata", name)
                }
            }
        }
    }
}
