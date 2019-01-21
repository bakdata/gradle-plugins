plugins {
    `java-library`
    idea
    kotlin("jvm") version "1.3.11"
    id("net.researchgate.release") version "2.6.0"
    id("org.sonarqube") version "2.7"
    id("io.codearte.nexus-staging") version "0.20.0"
    id("jacoco")
    id("org.jetbrains.dokka") version "0.9.17"
    id("signing")
    id("de.marcphilipp.nexus-publish") version "0.1.1"
}

jacoco {
    toolVersion = "0.8.2"
}

allprojects {
    apply(plugin = "jacoco")
    apply(plugin = "de.marcphilipp.nexus-publish")

    repositories {
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
        maven(url = "https://plugins.gradle.org/m2/")
    }
}

nexusStaging {
//    stagingProfileId = "8412378836ed9c"
    packageGroup = "com.bakdata"
    username = System.getenv("OSSRH_JIRA_USERNAME")
    password = System.getenv("OSSRH_JIRA_PASSWORD")
}

group = "com.bakdata.gradle"
val repoName: String by project
subprojects {
    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "signing")

    dependencies {
        "api"(gradleApi())
        "api"(gradleKotlinDsl())
        implementation(kotlin("stdlib"))
//        implementation(kotlin("gradle-plugin"))
    }

    tasks["jacocoTestReport"].dependsOn(tasks["test"])

    tasks.withType<Test> {
        testLogging {
            showStandardStreams = true
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
        from(sourceSets["main"])
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
                    description.set("sonatype-release is a plugin for easy Maven Central publishing of bakdata OSS projects using Gradle.")
                    name.set("${project.group}:${project.name}" as String)
                    url.set("http://github.com/bakdata/${repoName}")
                    organization {
                        name.set("bakdata.com")
                        url.set("https://github.com/bakdata")
                    }
                    issueManagement {
                        system.set("GitHub")
                        url.set("http://github.com/bakdata/${repoName}/issues")
                    }
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("http://github.com/bakdata/${repoName}/blob/master/LICENSE")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/bakdata/${repoName}.git")
                        developerConnection.set("scm:git:ssh://github.com:bakdata/${repoName}.git")
                        url.set("https://github.com/bakdata/${repoName}/")
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

//    tasks[PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME].dependsOn(tasks["sign"])
//    tasks[MavenPublishPlugin.PUBLISH_LOCAL_LIFECYCLE_TASK_NAME].dependsOn(tasks["sign"])
}


tasks {
    sonarqube {
        properties {
            property("sonar.projectName", "${repoName}")
            property("sonar.projectKey", "bakdata-${repoName}")
        }
    }
}

tasks["sonarqube"].dependsOn(tasks["jacocoTestReport"])

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