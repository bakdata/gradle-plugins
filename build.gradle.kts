buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("de.marcphilipp.gradle:nexus-publish-plugin:0.1.1")
    }
}

plugins {
    java
    `kotlin-dsl`
    kotlin("jvm") version "1.3.11"
    id("net.researchgate.release") version "2.6.0"
    id("org.sonarqube") version "2.7"
    id("io.codearte.nexus-staging") version "0.20.0"
    id("jacoco")
    id("org.jetbrains.dokka") version "0.9.17"
    id("signing")
//    id("de.marcphilipp.nexus-publish") version "0.1.1"
    id("com.gradle.plugin-publish") version "0.10.0"
}

allprojects(closureOf<Project> {
    apply(plugin = "jacoco")

    configure<JacocoPluginExtension> {
        toolVersion = "0.8.3"
    }

    repositories {
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
        maven(url = "https://plugins.gradle.org/m2/")
    }

    group = "com.bakdata.gradle"
})


configure<io.codearte.gradle.nexus.NexusStagingExtension> {
//    stagingProfileId = "8412378836ed9c"
    packageGroup = "com.bakdata"
    username = System.getenv("OSSRH_JIRA_USERNAME") ?: project.findProperty("ossrh.username")?.toString()
    password = System.getenv("OSSRH_JIRA_PASSWORD") ?: project.findProperty("ossrh.password")?.toString()
}

subprojects(closureOf<Project> {
    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "signing")
    apply(plugin = "org.gradle.kotlin.kotlin-dsl")

    apply(plugin = "de.marcphilipp.nexus-publish")

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

    tasks["jacocoTestReport"].dependsOn(tasks["test"])

    tasks.withType<Test> {
        testLogging {
            showStandardStreams = true
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
        useJUnitPlatform()
        systemProperty("java.util.logging.config.file", "src/test/resources/logging.properties")
    }

    val javadocJar by tasks.creating(Jar::class) {
        classifier = "javadoc"
        from(tasks["dokka"])
    }

    val sourcesJar by tasks.creating(Jar::class) {
        classifier = "sources"
        from(sourceSets["main"].allSource)
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                artifact(sourcesJar) {
                    classifier = "sources"
                }
                artifact(javadocJar) {
                    classifier = "javadoc"
                }

                pom {
                    description.set(project.description)
                    name.set("${project.group}:${project.name}")
                    url.set("http://github.com/bakdata/${project.name}")
                    organization {
                        name.set("bakdata.com")
                        url.set("https://github.com/bakdata")
                    }
                    issueManagement {
                        system.set("GitHub")
                        url.set("http://github.com/bakdata/${project.name}/issues")
                    }
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("http://github.com/bakdata/${project.name}/blob/master/LICENSE")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/bakdata/${project.name}.git")
                        developerConnection.set("scm:git:ssh://github.com:bakdata/${project.name}.git")
                        url.set("https://github.com/bakdata/${project.name}/")
                    }
                    developers {
                        developer {
                            name.set("Arvid Heise")
                            id.set("AHeise")
                        }
                    }
                }
            }
        }
    }

    configure<SigningExtension> {
        sign(the<PublishingExtension>().publications)
    }

    val build = tasks[LifecycleBasePlugin.BUILD_TASK_NAME]
    tasks[PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME].dependsOn(build)
    tasks[MavenPublishPlugin.PUBLISH_LOCAL_LIFECYCLE_TASK_NAME].dependsOn(build)
})

tasks {
    val jacocoMerge by creating(JacocoMerge::class) {
        dependsOn(subprojects.map { it.tasks["jacocoTestReport"] })
        executionData(subprojects.map { it.tasks["test"] })
    }

    register("publishToNexus") {
        dependsOn(subprojects.map { it.tasks["publishToNexus"] })
    }

    val jacocoMergeReport by creating(JacocoReport::class) {
        dependsOn(jacocoMerge)
        executionData(files(jacocoMerge.destinationFile))
        reports.xml.isEnabled = true
        sourceDirectories.from(subprojects.map { it.sourceSets["main"].allSource })
        classDirectories.from(subprojects.map { it.sourceSets["main"].output })
    }

    allprojects {
        configure<org.sonarqube.gradle.SonarQubeExtension> {
            properties {
//                property("sonar.jacoco.reportPaths", jacocoMergedReport.outputs)
                property("sonar.coverage.jacoco.xmlReportPaths", jacocoMergeReport.reports.xml.destination)
            }
        }
    }

    named("sonarqube") { dependsOn(jacocoMergeReport) }

}

//configure<org.sonarqube.gradle.SonarQubeExtension> {
//    properties {
//        property("sonar.jacoco.reportPaths", subprojects.map { it.tasks["jacocoTestReport"] }.joinToString(','))
//    }
//}

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

//gradle.taskGraph.whenReady {
//    if (hasTask(tasks["publishToNexus"]) && System.getenv("CI") == null) {
//        throw GradleException("Publishing artifacts is only supported through CI (e.g., Travis)")
//    }
//    if (hasTask(tasks["release"]) && System.getenv("CI") == null) {
//        throw GradleException("Release is only supported through CI (e.g., Travis)")
//    }
//    if (hasTask(tasks["closeAndReleaseRepository"]) && System.getenv("CI") == null) {
//        throw GradleException("Closing a release is only supported through CI (e.g., Travis)")
//    }
//}