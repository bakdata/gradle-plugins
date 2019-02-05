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

import de.marcphilipp.gradle.nexus.InitializeNexusStagingRepository
import io.codearte.gradle.nexus.NexusStagingExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import java.io.File
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

class SonatypePlugin : Plugin<Project> {
    override fun apply(rootProject: Project) {
        if(rootProject.parent != null) {
            throw GradleException("Apply this plugin only to the top-level project.")
        }

        with(rootProject) {
            allprojects {
                extensions.create<SonatypeSettings>("sonatype", this)
            }

            // lazy execution, so that settings configurations are actually used
            // note that we need to have afterEvaluate before applying the plugins (nexus-staging, nexus-publish),
            // so we can adjust the respective settings of the used plugins
            // (they use afterEvaluate to apply their settings in turn which is registered after ours)
            afterEvaluate {
                adjustConfiguration()
            }

            plugins.apply("base")
            plugins.apply("io.codearte.nexus-staging")

            getPublishableProjects().forEach { project ->
                addPublishTasks(project)
            }

            addParentPublishToNexusTasks()

            if (!project.getOverriddenSetting(SonatypeSettings::disallowLocalRelease)!!) {
                disallowPublishTasks()
            }
        }
    }

    /**
     * Recursively add publishToNexus (if not exists) which depends on the children.
     */
    private fun Project.addParentPublishToNexusTasks() {
        allprojects.forEach { project ->
            val parent = project.parent
            if (parent != null) {
                val provider =
                        try {
                            parent.tasks.named("publishToNexus")
                        } catch (e: UnknownTaskException) {
                            parent.tasks.register("publishToNexus")
                        }
                provider.configure {
                    dependsOn(project.tasks.named("publishToNexus"))
                }
            }
        }
    }

    private fun Project.adjustConfiguration() {
        // first try to set all settings, even if not given (yet)
        project.configure<NexusStagingExtension> {
            packageGroup = "com.bakdata"
            username = getOverriddenSetting(SonatypeSettings::osshrUsername)
            password = getOverriddenSetting(SonatypeSettings::osshrPassword)
        }

        allprojects {
            signExtras.forEach { (key, property) ->
                extra[key] = getOverriddenSetting(property)
            }
        }

        // verify that settings are really present when needed
        // note that we test the affected settings to allow users of the plugin to provide the values natively (e.g., directly on the used plugins)
        gradle.taskGraph.whenReady {
            val missingProps = mutableSetOf<KProperty1<SonatypeSettings, Any?>>()
            this.allTasks.filter { it is Sign }.forEach {
                missingProps += signExtras
                        .filter { (key, _) -> !it.project.extra.has(key) }
                        .map { it.value }
            }

            if(this.allTasks.any { it is AbstractPublishToMaven }) {
                project.configure<NexusStagingExtension> {
                    if(username == null) {
                        missingProps.add(SonatypeSettings::osshrUsername)
                    }
                    if(password == null) {
                        missingProps.add(SonatypeSettings::osshrPassword)
                    }
                }
            }

            this.allTasks.filter { it is GenerateMavenPom }.forEach {
                println("Adding pom information for ${it.project}")
                it.project.configure<PublishingExtension> {
                    publications.withType<MavenPublication> {
                        pom {
                            missingProps.addAll(addRequiredInformationToPom(it.project))
                        }
                    }
                }
            }

            if(missingProps.isNotEmpty()) {
                throw GradleException("Missing the following configurations ${missingProps.map { "sonatype.${it.name}" }} for ${project.name}")
            }
        }
    }

    private tailrec fun <T> Project.getOverriddenSetting(property: KProperty1<SonatypeSettings, T?>): T? =
            property.get(extensions.getByType(SonatypeSettings::class)) ?: project.parent?.getOverriddenSetting(property)

    private fun Project.getPublishableProjects() =
        allprojects.filter { it.subprojects.isEmpty() || File(it.projectDir, "src/main").exists() }

    private fun Project.disallowPublishTasks() {
        gradle.taskGraph.whenReady {
            if (hasTask(":publishToNexus") && System.getenv("CI") == null) {
                throw GradleException("Publishing artifacts is only supported through CI (e.g., Travis)")
            }
            if (hasTask(":release") && System.getenv("CI") == null) {
                throw GradleException("Release is only supported through CI (e.g., Travis)")
            }
            if (hasTask(":closeAndReleaseRepository") && System.getenv("CI") == null) {
                throw GradleException("Closing a release is only supported through CI (e.g., Travis)")
            }
        }
    }


    private fun addPublishTasks(project: Project) {
        with(project) {
            apply(plugin = "java")
            apply(plugin = "signing")
            apply(plugin = "de.marcphilipp.nexus-publish")

            repositories {
                maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
            }

            val javadocJar by tasks.creating(Jar::class) {
                archiveClassifier.set("javadoc")
                from(tasks.findByName("javadoc") ?: tasks.findByName("dokka"))
            }

            val sourcesJar by tasks.creating(Jar::class) {
                archiveClassifier.set("sources")
                from(project.the<SourceSetContainer>()["main"].allSource)
            }

            // java plugin already creates a MavenPublication, we try to extend it instead of creating a new one to avoid duplicate uploads.
            configure<PublishingExtension> {
               publications.create<MavenPublication>("sonatype") {
                    from(components["java"])
                    artifact(sourcesJar).classifier = "sources"
                    artifact(javadocJar).classifier = "javadoc"
                }
            }

            configure<SigningExtension> {
                sign(the<PublishingExtension>().publications)
            }
            tasks.named(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME) { dependsOn(tasks.withType<Sign>()) }
            tasks.named(MavenPublishPlugin.PUBLISH_LOCAL_LIFECYCLE_TASK_NAME) { dependsOn(tasks.withType<Sign>()) }
        }
    }

    private fun MavenPublication.addRequiredInformationToPom(project: Project): List<KMutableProperty1<SonatypeSettings, out Any?>> {
        val repoUrl = project.getOverriddenSetting(SonatypeSettings::repoUrl)
        val projectDescription = project.getOverriddenSetting(SonatypeSettings::description)
        val developers = project.getOverriddenSetting(SonatypeSettings::developers)

        val emptySettings = mapOf<Any?, KMutableProperty1<SonatypeSettings, *>>(
                repoUrl to SonatypeSettings::repoUrl,
                projectDescription to SonatypeSettings::description,
                developers to (SonatypeSettings::developers))
                .filter { (key, _) -> key ==null }
                .map { it.value }
        if(emptySettings.isNotEmpty()) {
            return emptySettings
        }

        pom.apply {
            description.set(projectDescription)
            name.set("${project.group}:${project.name}")
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
                connection.set("scm:git:${repoUrl!!.replace("^https?".toRegex(), "git")}.git")
                developerConnection.set("scm:git:${repoUrl.replace("^https?".toRegex(), "ssh")}.git")
                url.set(repoUrl)
            }
            developers(developers)
        }

        return listOf()
    }

    companion object {
        private val signExtras = mapOf("signing.keyId" to SonatypeSettings::signingKeyId,
                "signing.password" to SonatypeSettings::signingPassword,
                "signing.secretKeyRingFile" to SonatypeSettings::signingSecretKeyRingFile)
    }
}
