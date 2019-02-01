
buildscript {
    repositories {
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("com.bakdata.gradle:sonatype:1.0.1-SNAPSHOT")
    }
    configurations.all {
        resolutionStrategy.cacheChangingModulesFor(0, "seconds")
    }
}

plugins {
    // kotlin stuff
    `kotlin-dsl`
    kotlin("jvm") version "1.3.11"
    id("org.jetbrains.dokka") version "0.9.17"
    // release
    id("net.researchgate.release") version "2.6.0"
    // eat your own dog food - apply the plugins to this plugin project
    id("com.bakdata.sonar") version "1.0.0"
//    id("com.bakdata.sonatype") version "1.0.0"

    id("com.gradle.plugin-publish") version "0.10.1"
}
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
    subprojects.forEach { project ->
        with(project) {
            apply(plugin = "java-gradle-plugin")
            apply(plugin = "com.gradle.plugin-publish")
            project.afterEvaluate {
                configure<GradlePluginDevelopmentExtension> {
                    plugins {
                        create("${project.name.capitalize()}Plugin") {
                            id = "com.bakdata.${project.name}"
                            implementationClass = "com.bakdata.gradle.${project.name.capitalize()}Plugin"
                            description = project.description
                        }
                    }
                }
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
    }
}
