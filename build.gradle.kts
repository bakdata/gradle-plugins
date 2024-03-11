plugins {
    // release
    id("com.bakdata.release") version "1.3.0"
    // eat your own dog food - apply the plugins to this plugin project
    id("com.bakdata.sonar") version "1.1.17"
    id("com.bakdata.sonatype") version "1.2.2"
    id("org.gradle.kotlin.kotlin-dsl") version "2.1.6" apply false
    id("com.gradle.plugin-publish") version "1.2.1" apply false
}

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
    developers {
        developer {
            name.set("Arvid Heise")
            id.set("AHeise")
        }
        developer {
            name.set("Philipp Schirmer")
            id.set("philipp94831")
        }
        developer {
            name.set("Torben Meyer")
            id.set("torbsto")
        }
    }
}

subprojects {
    apply(plugin = "java")

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    apply(plugin = "java-gradle-plugin")

    // config for gradle plugin portal doesn't support snapshot, so we add config only if release version
    if (!version.toString().endsWith("-SNAPSHOT")) {
        apply(plugin = "com.gradle.plugin-publish")
    }

    // description is only ready after evaluation
    afterEvaluate {
        configure<GradlePluginDevelopmentExtension> {
            plugins {
                create("${project.name.capitalize()}Plugin") {
                    id = "com.bakdata.${project.name}"
                    implementationClass = "com.bakdata.gradle.${project.name.capitalize()}Plugin"
                    description = project.description
                    displayName = "Bakdata $name plugin"
                }
            }
        }

        extensions.findByType(com.gradle.publish.PluginBundleExtension::class)?.apply {
            // actual block of plugin portal config, need to be done on each subproject as the plugin does not support multi-module projects yet...
            website = "https://github.com/bakdata/gradle-plugins"
            vcsUrl = "https://github.com/bakdata/gradle-plugins"
            tags = listOf("bakdata", name)
        }
    }

    dependencies {
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.3.0")
        "testImplementation"("org.junit.jupiter:junit-jupiter-api:5.3.0")
        "testImplementation"("org.assertj", "assertj-core", "3.11.1")
        "testImplementation"("org.junit-pioneer", "junit-pioneer", "0.3.0")
    }
}

val sonarqube by tasks
sonarqube.enabled = false //FIXME requires Java 17
