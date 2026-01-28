import java.util.*

plugins {
    // eat your own dog food - apply the plugins to this plugin project
    id("com.bakdata.release") version "2.0.0"
    id("com.bakdata.sonar") version "2.0.0"
    id("com.bakdata.sonatype") version "2.0.1-SNAPSHOT"
    id("org.gradle.kotlin.kotlin-dsl") version "6.4.2" apply false
    id("com.gradle.plugin-publish") version "2.0.0" apply false
    id("org.jetbrains.dokka-javadoc") version "2.1.0" apply false
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
    apply(plugin = "org.jetbrains.dokka-javadoc")

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
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
        "testImplementation"("org.junit.jupiter:junit-jupiter:6.0.2")
        "testImplementation"("org.assertj:assertj-core:3.27.7")
    }
}

fun capitalize(it: Char) = if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
