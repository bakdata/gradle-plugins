import java.util.*

plugins {
    // eat your own dog food - apply the plugins to this plugin project
    alias(libs.plugins.release)
    alias(libs.plugins.sonar)
    alias(libs.plugins.sonatype)
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.plugin.publish) apply false
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

subprojects {
    apply(plugin = "java")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
    }

    publication {
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

        //will be created by Gradle Plugin Development Plugin
        createPublication = false
    }

    apply(plugin = "java-gradle-plugin")

    // config for Gradle plugin portal doesn't support snapshot, so we add config only if release version
    if (!version.toString().endsWith("-SNAPSHOT")) {
        apply(plugin = "com.gradle.plugin-publish")
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
        "testRuntimeOnly"(rootProject.libs.junit.platform.launcher)
        "testImplementation"(rootProject.libs.junit.jupiter)
        "testImplementation"(rootProject.libs.assertj)
    }
}

fun capitalize(it: Char) = if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
