import java.util.*

plugins {
    // eat your own dog food - apply the plugins to this plugin project
    id("com.bakdata.release") version "1.7.1"
    id("com.bakdata.sonar") version "1.7.1"
    id("com.bakdata.sonatype") version "1.8.1-SNAPSHOT"
    id("org.gradle.kotlin.kotlin-dsl") version "5.1.2" apply false
    id("com.gradle.plugin-publish") version "1.3.0" apply false
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
        developer {
            name.set("Ramin Gharib")
            id.set("raminqaf")
        }
    }
}

subprojects {
    apply(plugin = "java")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(11)
        }
    }

    apply(plugin = "java-gradle-plugin")

    // config for gradle plugin portal doesn't support snapshot, so we add config only if release version
    if (!version.toString().endsWith("-SNAPSHOT")) {
        apply(plugin = "com.gradle.plugin-publish")
    }

    configure<com.bakdata.gradle.SonatypeSettings> {
        // created by gradle plugin development plugin
        createPublication = false
    }

    val pluginName = "${project.name.replaceFirstChar(::capitalize)}Plugin"
    configure<GradlePluginDevelopmentExtension> {
        website.set("https://github.com/bakdata/gradle-plugins")
        vcsUrl.set("https://github.com/bakdata/gradle-plugins")
        plugins {
            create(pluginName) {
                id = "com.bakdata.${project.name}"
                implementationClass = "com.bakdata.gradle.${project.name.replaceFirstChar(::capitalize)}Plugin"
                displayName = "Bakdata $name plugin"
                tags = listOf("bakdata", name)
            }
        }
    }
    // description is only ready after evaluation
    afterEvaluate {
        configure<GradlePluginDevelopmentExtension> {
            plugins {
                getByName(pluginName) {
                    description = project.description
                }
            }
        }
    }

    dependencies {
        val junitVersion: String by project
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
        "testImplementation"("org.junit.jupiter:junit-jupiter-api:$junitVersion")
        "testImplementation"("org.assertj", "assertj-core", "3.27.2")
    }
}

fun capitalize(it: Char) = if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
