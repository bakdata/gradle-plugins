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

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
//import org.gradle.testing.jacoco.tasks.JacocoMerge

import org.gradle.kotlin.dsl.*
import org.gradle.testing.jacoco.tasks.JacocoMerge
import org.sonarqube.gradle.SonarQubeExtension
import java.io.File

class SonarPlugin : Plugin<Project> {
    override fun apply(rootProject: Project) {
        if(rootProject.parent != null) {
            throw GradleException("Apply this plugin only to the top-level project.")
        }

        with(rootProject) {
            plugins.apply("org.sonarqube")

            if(subprojects.isEmpty()) {
                plugins.apply("java")
                plugins.apply("jacoco")
                tasks.named("jacocoTestReport") { dependsOn(tasks.named("test")) }
                tasks.named("sonarqube") { dependsOn(tasks.named("jacocoTestReport")) }
            }
            else {
                // setup code coverage in a way that we measure covered code lines across all submodules
                // thus an API only module gets it's coverage by tests in an implementation module
                allprojects {
                    plugins.apply("java")
                    plugins.apply("jacoco")
                }

                // then merge all reports
                val jacocoMerge by tasks.registering(JacocoMerge::class) {
                    subprojects.forEach { subproject ->
                        dependsOn(subproject.tasks["jacocoTestReport"])
                        executionData(subproject.tasks["test"])
                    }
                }
                configure<SonarQubeExtension> {
                    properties {
                        property("sonar.projectName", rootProject.name)
                        property("sonar.projectKey", "bakdata-${rootProject.name}")
                        property("sonar.java.coveragePlugin", "jacoco")
                        property("sonar.jacoco.reportPaths", jacocoMerge.get().destinationFile)
                    }
                }
                // and finally use that as an input for sonar
                tasks.named("sonarqube") { dependsOn(jacocoMerge) }
            }
        }
    }
}