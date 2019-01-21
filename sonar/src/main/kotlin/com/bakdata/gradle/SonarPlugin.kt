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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoMerge

import org.gradle.kotlin.dsl.*
import org.sonarqube.gradle.SonarQubeExtension

class SonarPlugin : Plugin<Project> {
    override fun apply(rootProject: Project) {
        rootProject.apply(closureOf<Project> {
            plugins.apply("base")
            plugins.apply("org.sonarqube")
            plugins.apply("jacoco")

            configure<JacocoPluginExtension> {
                toolVersion = "0.8.2"
            }

            if(subprojects.isEmpty()) {
                tasks["jacocoTestReport"].dependsOn(tasks["test"])
                tasks["sonarqube"].dependsOn(tasks["jacocoTestReport"])
            }
            else {
                // setup code coverage in a way that we measure covered code lines across all submodules
                // thus an API only module gets it's coverage by tests in an implementation module
                subprojects(closureOf<Project> {
                    // run tests first
                    // then jacocoTestReport
                    tasks["jacocoTestReport"].dependsOn(tasks["test"])

                    // then merge all reports
                    val jacocoMerge by tasks.creating(JacocoMerge::class) {
                        dependsOn(subprojects.map { it.tasks["jacocoTestReport"] })
                        executionData(subprojects.map { it.tasks["test"] })
                    }

                    // and finally use that as an input for sonar
                    tasks["sonarqube"].dependsOn(jacocoMerge)
                    configure<SonarQubeExtension> {
                        properties {
                            it.property("sonar.projectName", rootProject.name)
                            it.property("sonar.projectKey", "bakdata-${rootProject.name}")
                            it.property("sonar.jacoco.reportPaths", jacocoMerge.destinationFile)
                        }
                    }
                })
            }
        })
    }
}