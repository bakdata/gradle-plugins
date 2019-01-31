buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    }
    dependencies {
        classpath("com.bakdata.gradle:sonar:0.9.9-SNAPSHOT")
        classpath("com.bakdata.gradle:sonatype:0.9.9-SNAPSHOT")
    }
    configurations.all {
        resolutionStrategy.cacheChangingModulesFor(0, "seconds")
    }
}

plugins {
    `kotlin-dsl`
    kotlin("jvm") version "1.3.11"
    id("net.researchgate.release") version "2.6.0"
    id("org.jetbrains.dokka") version "0.9.17"
    id("com.gradle.plugin-publish") version "0.10.0"
}

apply(plugin = "com.bakdata.sonar")
apply(plugin = "com.bakdata.sonatype")

allprojects {
    repositories {
        maven(url = "https://plugins.gradle.org/m2/")
    }

    group = "com.bakdata.gradle"

    tasks.withType<Test> {
        maxParallelForks = 4
    }
}

configure<com.bakdata.gradle.SonatypeSettings> {
    disallowLocalRelease = false
    description = ""
    developers {
        developer {
            name.set("Arvid Heise")
            id.set("AHeise")
        }
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "org.gradle.kotlin.kotlin-dsl")

    configure<org.gradle.kotlin.dsl.plugins.dsl.KotlinDslPluginOptions> {
        experimentalWarning.set(false)
    }

    dependencies {
        "api"(gradleApi())
        "api"(gradleKotlinDsl())
        implementation(kotlin("stdlib"))

        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.0")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.0")
        testImplementation("org.assertj", "assertj-core", "3.11.1")
        testImplementation("org.junit-pioneer", "junit-pioneer", "0.3.0")
    }
}

// config for gradle plugin portal
// doesn't support snapshot, so we add config only if release version
if(!version.toString().endsWith("-SNAPSHOT")) {
    configure<com.gradle.publish.PluginBundleExtension> {
        website = "https://github.com/bakdata/gradle-plugins"
        vcsUrl = "https://github.com/bakdata/gradle-plugins"

        description = "Greetings from here!"

        (plugins) {
            subprojects.forEach { project ->
                "${project.name.capitalize()}Plugin" {
                    id = "com.bakdata.${project.name}"
                    displayName = "Bakdata ${project.name} plugin"
                    description = "Provides basic integration with ${project.name} for (multi-module) projects"
                }
            }
        }
    }
}
