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

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

class SonarPlugin : Plugin<Project> {
    private val log = Logging.getLogger(SonarPlugin::class.java)

    override fun apply(rootProject: Project) {
        if (rootProject.parent != null) {
            throw GradleException("Apply this plugin only to the top-level project.")
        }

        with(rootProject) {
            apply(plugin = "org.sonarqube")

            allprojects {
                plugins.withType<JavaPlugin> {
                    log.info("Found java component in $project. Adding jacoco and wiring to sonarqube.")

                    project.apply(plugin = "jacoco")

                    project.tasks.withType<Test> {
                        testLogging {
                            showStandardStreams = true

                            events("passed", "skipped", "failed")
                            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                        }
                        useJUnitPlatform()
                        systemProperty("java.util.logging.config.file", "src/test/resources/logging.properties")
                    }

                    project.configure<JacocoPluginExtension> {
                        // smaller versions won't work with kotlin properly
                        toolVersion = "0.8.12"
                    }

                    tasks.withType<JacocoReport> {
                        reports.xml.required.set(true)
                    }

                    rootProject.tasks.named("sonar") {
                        dependsOn(
                            tasks.withType<JacocoReport>(),
                            tasks.withType<Test>()
                        )
                    }
                }
            }
        }
    }
}
