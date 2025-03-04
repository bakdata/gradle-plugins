/*
 * The MIT License
 *
 * Copyright (c) 2025 bakdata GmbH
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

import io.github.gradlenexus.publishplugin.NexusPublishExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import java.net.URI
import java.time.Duration
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

class SonatypePlugin : Plugin<Project> {
    private val log = Logging.getLogger(SonatypePlugin::class.java)

    override fun apply(rootProject: Project) {
        if (rootProject.parent != null) {
            throw GradleException("Apply this plugin only to the top-level project.")
        }

        with(rootProject) {
            extensions.create<SonatypeSettings>("sonatype", this)
            allprojects {
                extensions.create<PublicationSettings>("publication", this)
            }

            // note that we need to use adjustConfiguration before applying the plugin (publish-plugin),
            // so we can adjust the respective settings of the used plugins
            // (they use afterEvaluate to apply their settings in turn which is registered after ours)
            adjustConfiguration()

            plugins.apply("base")
            plugins.apply("io.github.gradle-nexus.publish-plugin")

            configure<NexusPublishExtension> {
                // create default repository called 'nexus' and set the corresponding default urls
                repositories.create("nexus") {
                    nexusUrl.set(URI.create("https://s01.oss.sonatype.org/service/local/"))
                    snapshotRepositoryUrl.set(URI.create("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
                }
            }

            allprojects {
                project.plugins.matching { it is JavaPlugin }.all {
                    if (extensions.findByType<PublishingExtension>() == null) {
                        log.info("Found java component in $project. Adding publishing tasks.")
                        addPublishTasks(project)
                    }
                }
            }

            disallowPublishTasks()
        }
    }

    private fun Project.adjustConfiguration() {
        allprojects {
            val settings = getSettings()
            signExtras.forEach { (key, property) ->
                extra[key] = property.get(settings)
            }
        }

        // lazy execution, so that settings configurations are actually used
        afterEvaluate {
            // first try to set all settings, even if not given (yet)
            project.configure<NexusPublishExtension> {
                packageGroup.set("com.bakdata")

                repositories["nexus"].apply {
                    stagingProfileId.set("746f6fd1d91a4")
                    val settings = getSettings()
                    username.set(settings.osshrUsername)
                    password.set(settings.osshrPassword)
                    settings.nexusUrl?.let { nexusUrl.set(uri(it)) }
                    allowInsecureProtocol.set(settings.allowInsecureProtocol)
                }
            }
        }

        // verify that settings are really present when needed
        // note that we test the affected settings to allow users of the plugin to provide the values natively (e.g., directly on the used plugins)
        gradle.taskGraph.whenReady {
            val missingProps = mutableSetOf<KProperty1<SonatypeSettings, Any?>>()

            val onlyLocalPublish = this.allTasks
                .filterIsInstance<AbstractPublishToMaven>()
                .all { it is PublishToMavenLocal }

            this.allTasks.filterIsInstance<Sign>().forEach {
                // disable sign for publishToLocalMaven
                it.onlyIf { !onlyLocalPublish }

                val settings = getSettings()
                signExtras.forEach { (key, property) ->
                    if (it.project.extra[key] == null) {
                        val value = property.get(settings)
                        if (value == null) {
                            missingProps += property
                        } else {
                            it.project.extra[key] = value
                        }
                    }
                }
            }

            if (this.allTasks.any { it is AbstractPublishToMaven }) {
                project.configure<NexusPublishExtension> {
                    repositories["nexus"].apply {
                        if (!username.isPresent) {
                            missingProps.add(SonatypeSettings::osshrUsername)
                        }
                        if (!password.isPresent) {
                            missingProps.add(SonatypeSettings::osshrPassword)
                        }
                        getSettings().nexusUrl?.let { nexusUrl.set(uri(it)) }
                    }
                }

                allprojects {
                    extensions.findByType<NexusPublishExtension>()?.let { nexus ->
                        val settings = getSettings()
                        settings.nexusUrl?.let {
                            nexus.repositories["nexus"].nexusUrl.value(uri(it))
                        }

                        settings.snapshotUrl?.let {
                            nexus.repositories["nexus"].snapshotRepositoryUrl.value(uri(it))
                        }

                        settings.clientTimeout.let {
                            nexus.clientTimeout.value(Duration.ofSeconds(it))
                        }

                        settings.connectTimeout.let {
                            nexus.connectTimeout.value(Duration.ofSeconds(it))
                        }

                        settings.allowInsecureProtocol.let {
                            nexus.repositories["nexus"].allowInsecureProtocol.value(it)
                        }
                    }
                }
            }

            this.allTasks.filterIsInstance<GenerateMavenPom>().forEach { generateMavenPom ->
                log.info("Adding pom information for ${generateMavenPom.project}")
                generateMavenPom.project.configure<PublishingExtension> {
                    publications.withType<MavenPublication> {
                        pom {
                            val pomMissingProps = addRequiredInformationToPom(generateMavenPom.project)
                            if (pomMissingProps.isNotEmpty()) {
                                throw GradleException("Missing the following configurations ${pomMissingProps.map { "publication.${it.name}" }} for ${project.name}")
                            }
                        }
                    }
                }
            }

            logNexusPublishingSetting()
            // disable check if we only do a publishToLocalMaven as no credentials are required
            if (!onlyLocalPublish && missingProps.isNotEmpty()) {
                throw GradleException("Missing the following configurations ${missingProps.map { "sonatype.${it.name}" }} for ${project.name}")
            }
        }
    }

    private tailrec fun Project.getSettings(): SonatypeSettings =
        extensions.findByType<SonatypeSettings>() ?: project.parent!!.getSettings()

    private fun Project.getPublicationSettings(): PublicationSettings =
        extensions.getByType<PublicationSettings>()

    private fun Project.disallowPublishTasks() {
        gradle.taskGraph.whenReady {
            if (getSettings().disallowLocalRelease) {
                log.info("disallowing publish tasks")
                if (hasTask(":publishToNexus") && System.getenv("CI") == null) {
                    throw GradleException("Publishing artifacts is only supported through CI (e.g., Travis)")
                }
                if (hasTask(":release") && System.getenv("CI") == null) {
                    throw GradleException("Release is only supported through CI (e.g., Travis)")
                }
                if (hasTask(":closeAndReleaseStagingRepositories") && System.getenv("CI") == null) {
                    throw GradleException("Closing a release is only supported through CI (e.g., Travis)")
                }
            }
        }
    }


    private fun addPublishTasks(project: Project) {
        with(project) {
            apply(plugin = "signing")
            apply(plugin = "org.gradle.maven-publish")

            project.plugins.matching { it is JavaPlugin }.all {
                configure<JavaPluginExtension> {
                    withSourcesJar()
                    withJavadocJar()
                }

                project.tasks.matching { it.name == "dokkaJavadoc" }.all {
                    val javadocTask: Task = this
                    tasks.named<Jar>("javadocJar") {
                        from(javadocTask)
                    }
                }
            }

            // lazy execution, so that settings configurations are actually used
            afterEvaluate {
                if (project.getPublicationSettings().createPublication) {
                    configure<PublishingExtension> {
                        publications.create<MavenPublication>("sonatype") {
                            from(components["java"])
                        }
                    }
                }
            }

            configure<SigningExtension> {
                sign(the<PublishingExtension>().publications)
            }

            tasks.matching { it is AbstractPublishToMaven }.all { dependsOn(tasks.withType<Sign>()) }
        }
    }

    private fun MavenPublication.addRequiredInformationToPom(project: Project): List<KMutableProperty1<PublicationSettings, out Any?>> {
        val publicationSettings = project.getPublicationSettings()
        val repoUrl = publicationSettings.repoUrl
        val projectDescription = publicationSettings.description
        val developers = publicationSettings.developers

        val emptySettings = mapOf<Any?, KMutableProperty1<PublicationSettings, *>>(
            repoUrl to PublicationSettings::repoUrl,
            projectDescription to PublicationSettings::description,
            developers to (PublicationSettings::developers)
        )
            .filter { (key, _) -> key == null }
            .map { it.value }
        if (emptySettings.isNotEmpty()) {
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

    private fun Project.logNexusPublishingSetting() {
        extensions.findByType(NexusPublishExtension::class)?.let {
            project.logger.debug(
                "Publish to Nexus (${it.repositories["nexus"].nexusUrl.get()}) " +
                        "with connect timeout of ${it.connectTimeout.get()} " +
                        "and client timeout of ${it.clientTimeout.get()}"
            )
        }
    }

    companion object {
        private val signExtras = mapOf(
            "signing.keyId" to SonatypeSettings::signingKeyId,
            "signing.password" to SonatypeSettings::signingPassword,
            "signing.secretKeyRingFile" to SonatypeSettings::signingSecretKeyRingFile
        )
    }
}
