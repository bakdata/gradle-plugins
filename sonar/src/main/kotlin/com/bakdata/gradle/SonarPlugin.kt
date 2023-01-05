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

//import org.gradle.testing.jacoco.tasks.JacocoMerge

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoMerge
import org.gradle.testing.jacoco.tasks.JacocoReport

class SonarPlugin : Plugin<Project> {
    private val log = Logging.getLogger(SonarPlugin::class.java)

    override fun apply(rootProject: Project) {
        if (rootProject.parent != null) {
            throw GradleException("Apply this plugin only to the top-level project.")
        }

        with(rootProject) {
            plugins.apply("org.sonarqube")

            allprojects {
                plugins.withType(JavaPlugin::class.java) {
                    log.info("Found java component in $project. Adding jacoco and wiring to sonarqube.")

                    project.plugins.apply("jacoco")

                    project.tasks.withType(Test::class.java) {
                        it.testLogging { logging ->
                            logging.showStandardStreams = true

                            logging.events("passed", "skipped", "failed")
                            logging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                        }
                        it.useJUnitPlatform()
                        it.systemProperty("java.util.logging.config.file", "src/test/resources/logging.properties")
                    }

                    project.configure<JacocoPluginExtension> {
                        // smaller versions won't work with kotlin properly
                        it.toolVersion = "0.8.3"
                    }

                    tasks.withType(JacocoReport::class.java) {
                        it.reports.xml.required.set(true)
                    }

                    rootProject.tasks.named("sonarqube") { it.dependsOn(tasks.withType(JacocoReport::class.java), tasks.withType(Test::class.java)) }
                }
            }

            if (!subprojects.isEmpty()) {
                project.getTasks().register("jacocoMerge", JacocoMerge.class, task -> {
                    subprojects {
                        executionData(tasks.withType<JacocoReport>().map { it.executionData })
                    }
                    destinationFile = file("$buildDir/jacoco")
                }
                tasks.register<JacocoReport>("jacocoRootReport") {
                    dependsOn(jacocoMerge)
                    sourceDirectories.from(files(subprojects.map {
                        it.the<SourceSetContainer>()["main"].allSource.srcDirs
                    }))
                    classDirectories.from(files(subprojects.map { it.the<SourceSetContainer>()["main"].output }))
                    executionData(jacocoMerge.get().destinationFile)
                    reports {
                        html.isEnabled = true
                        xml.isEnabled = true
                        csv.isEnabled = false
                    }
                }

                // using a newer feature of sonarqube to use the xml reports which also makes it language-agnostic
                configure<org.sonarqube.gradle.SonarQubeExtension> {
                    properties {
                        property("sonar.coverage.jacoco.xmlReportPaths",
                                rootProject.tasks.withType<JacocoReport>().map { it.reports.xml.destination })
                    }
                }
            }
        }
    }
}
