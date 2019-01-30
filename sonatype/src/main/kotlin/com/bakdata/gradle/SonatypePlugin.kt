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
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.internal.provider.Providers
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import kotlin.reflect.KProperty1

class SonatypePlugin : Plugin<Project> {
    override fun apply(rootProject: Project) {
        if(rootProject.parent != null) {
            throw GradleException("Apply this plugin only to the top-level project.")
        }

        with(rootProject) {
            val rootSettings = extensions.create<SonatypeSettings>("sonatype", rootProject)
            subprojects {
                extensions.create<SonatypeSettings>("sonatype", rootProject)
            }

            // lazy execution, so that settings configurations are actually used
            afterEvaluate {
                configure<NexusStagingExtension> {
                    packageGroup = "com.bakdata"
                    username = requireNotNull(rootSettings.osshrJiraUsername) { "sonatype.osshrJiraUsername not set" }
                    password = requireNotNull(rootSettings.osshrJiraPassword) { "sonatype.osshrJiraPassword not set" }
                }

                val projects = if (subprojects.isEmpty()) listOf(rootProject) else subprojects
                projects.forEach { project ->
                    addPublishTasks(project)
                }

                if (rootSettings.disallowLocalRelease) {
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

            plugins.apply("base")
            plugins.apply("io.codearte.nexus-staging")

            allprojects {
                plugins.apply("de.marcphilipp.nexus-publish")
                repositories {
                    mavenCentral()
                    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
                }
            }
        }
    }

    private tailrec fun <T> Project.getOverriddenSetting(property: KProperty1<SonatypeSettings, T?>): T? =
            property.get(extensions.getByType(SonatypeSettings::class)) ?: project.parent?.getOverriddenSetting(property)

    private fun <T> Project.getRequiredSetting(property: KProperty1<SonatypeSettings, T?>): T =
            requireNotNull(project.getOverriddenSetting(property)) { "sonatype.${property.name} not set "}

    private fun addPublishTasks(project: Project) {
        with(project) {
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
                        addRequiredInformationToPom(project)
                    }
                }
            }

            configure<SigningExtension> {
                sign(the<PublishingExtension>().publications)
                extra["signing.keyId"] = project.getRequiredSetting(SonatypeSettings::signingKeyId)
                extra["signing.password"] = project.getRequiredSetting(SonatypeSettings::signingPassword)
                extra["signing.secretKeyRingFile"] = project.getRequiredSetting(SonatypeSettings::signingSecretKeyRingFile)
            }

            tasks[PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME].dependsOn(tasks["signMavenPublication"])
            tasks[MavenPublishPlugin.PUBLISH_LOCAL_LIFECYCLE_TASK_NAME].dependsOn(tasks["signMavenPublication"])
        }
    }

    private fun MavenPublication.addRequiredInformationToPom(project: Project) {
        pom.apply {
            description.set(Providers.of(project.getRequiredSetting(SonatypeSettings::description)))
            name.set("${project.group}:${project.name}")
            val repoUrl = project.getRequiredSetting(SonatypeSettings::repoUrl)
            url.set(repoUrl)
            organization {
                name.set("bakdata.com")
                url.set("https://github.com/bakdata")
            }
            issueManagement {
                system.set("GitHub")
                url.set("$repoUrl/issues")
            }
            licenses {
                license {
                    name.set("MIT License")
                    url.set("$repoUrl/blob/master/LICENSE")
                }
            }
            scm {
                connection.set("scm:git:${repoUrl.replace("^https?".toRegex(), "git")}.git")
                developerConnection.set("scm:git:${repoUrl.replace("^https?".toRegex(), "ssh")}.git")
                url.set(repoUrl)
            }
            developers(project.getRequiredSetting(SonatypeSettings::developers))
        }
    }
}