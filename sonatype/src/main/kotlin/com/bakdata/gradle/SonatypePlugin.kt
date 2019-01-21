/*
 * The MIT License
 *
 * Copyright (c) 2019 bakdata GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.bakdata.gradle

import io.codearte.gradle.nexus.NexusStagingExtension
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPomDeveloperSpec
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension

data class SonatypeSettings(
        val project: Project,
        var disallowLocalRelease: Boolean = true,
        var osshrJiraUsername: String = System.getenv("OSSRH_JIRA_USERNAME"),
        var osshrJiraPassword: String = System.getenv("OSSRH_JIRA_PASSWORD"),
        var repoName: String = project.name,
        var repoUrl: String = "https://github.com/bakdata/${repoName}",
        var mavenPomDeveloperSpec: Action<in MavenPomDeveloperSpec>? = null)

class SonatypePlugin : Plugin<Project> {
    override fun apply(rootProject: Project) {
        val settings = SonatypeSettings(project = rootProject)
        rootProject.extensions.add("sonatype", settings)

        rootProject.apply(closureOf<Project> {
            plugins.apply("base")
            plugins.apply("io.codearte.nexus-staging")

            allprojects(closureOf<Project> {
                plugins.apply("de.marcphilipp.nexus-publish")
                repositories {
                    mavenCentral()
                    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
                }
            })

            afterEvaluate {
                configure<NexusStagingExtension> {
                    packageGroup = "com.bakdata"
                    username = settings.osshrJiraUsername
                    password = settings.osshrJiraPassword
                }

                val projects = if (subprojects.isEmpty()) listOf(rootProject) else subprojects
                projects.forEach { project ->
                    addPublishTasks(project, settings)
                }

                if (settings.disallowLocalRelease) {
                    gradle.taskGraph.whenReady(closureOf<TaskExecutionGraph> {
                        if (hasTask(tasks["publishToNexus"]) && System.getenv("CI") == null) {
                            throw GradleException("Publishing artifacts is only supported through CI (e.g., Travis)")
                        }
                        if (hasTask(tasks["release"]) && System.getenv("CI") == null) {
                            throw GradleException("Release is only supported through CI (e.g., Travis)")
                        }
                        if (hasTask(tasks["closeAndReleaseRepository"]) && System.getenv("CI") == null) {
                            throw GradleException("Closing a release is only supported through CI (e.g., Travis)")
                        }
                    })
                }
            }
        })
    }

    private fun addPublishTasks(project: Project, settings: SonatypeSettings) {
        project.apply(closureOf<Project> {
            apply(plugin = "java")
            apply(plugin = "signing")

            val javadocJar by tasks.creating(Jar::class) {
                archiveClassifier.set("javadoc")
                from(tasks.findByPath("javadoc") ?: tasks.findByPath("dokka"))
            }

            val sourcesJar by tasks.creating(Jar::class) {
                archiveClassifier.set("sources")
                from(project.the<SourceSetContainer>()["main"].allSource)
            }

            configure<PublishingExtension> {
                publications.create<MavenPublication>("maven") {
                    from(components["java"])
                    artifact(sourcesJar).classifier = "sources"
                    artifact(javadocJar).classifier = "javadoc"

                    pom {
                        addRequiredInformationToPom(pom, project, settings)
                    }
                }
            }

            tasks[PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME].dependsOn(tasks["signArchives"])
            tasks[MavenPublishPlugin.PUBLISH_LOCAL_LIFECYCLE_TASK_NAME].dependsOn(tasks["signArchives"])

            configure<SigningExtension> {
                sign(the<PublishingExtension>().publications)
            }
        })
    }

    private fun MavenPublication.addRequiredInformationToPom(pom: MavenPom, project: Project, settings: SonatypeSettings) {
        pom.apply {
            description.set("Java DSL for (online) deduplication")
            name.set("${project.group}:${project.name}")
            url.set(settings.repoUrl)
            organization {
                it.name.set("bakdata.com")
                it.url.set("https://github.com/bakdata")
            }
            issueManagement {
                it.system.set("GitHub")
                it.url.set("${settings.repoUrl}/issues")
            }
            licenses {
                it.license {
                    name.set("MIT License")
                    url.set("${settings.repoUrl}/blob/master/LICENSE")
                }
            }
            scm {
                it.connection.set("scm:git:${settings.repoName.replace("^https?".toRegex(), "git")}.git")
                it.developerConnection.set("scm:git:${settings.repoName.replace("^https?".toRegex(), "ssh")}.git")
                it.url.set("${settings.repoUrl}")
            }
            developers(settings.mavenPomDeveloperSpec)
        }
    }
}