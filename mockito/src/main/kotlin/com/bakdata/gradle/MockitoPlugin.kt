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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.newInstance
import org.gradle.process.CommandLineArgumentProvider

class MockitoPlugin : Plugin<Project> {
    private val log = Logging.getLogger(MockitoPlugin::class.java)

    override fun apply(project: Project) {
        // according to https://javadoc.io/doc/org.mockito/mockito-core/5.15.2/org/mockito/Mockito.html#0.3
        project.configurations.matching { it.name == "testRuntimeClasspath" }.all {
            val testRuntimeClasspath = this
            val tests = project.tasks.withType(Test::class.java)
            val configureMockitoAgent = project.tasks.register("configureMockitoAgent") {
                dependsOn(testRuntimeClasspath)
                val resolvedConfiguration = testRuntimeClasspath.resolvedConfiguration
                val resolvedArtifacts = resolvedConfiguration.resolvedArtifacts
                resolvedArtifacts.find { isMockitoCoreWithAgent(it) }
                    ?.let { mockito ->
                        tests.forEach {
                            log.info("Configuring Mockito java agent for task ${it.name}")
                            it.jvmArgumentProviders.add(
                                project.objects.newInstance<JavaAgentArgumentProvider>().apply {
                                    classpath.from(mockito.file)
                                }
                            )
                        }
                    }
            }
            tests.configureEach {
                dependsOn(configureMockitoAgent)
            }
        }
    }

    private fun isMockitoCoreWithAgent(artifact: ResolvedArtifact): Boolean {
        val versionIdentifier = artifact.moduleVersion.id
        if (versionIdentifier.module.toString() != "org.mockito:mockito-core") {
            return false
        }
        val versions = versionIdentifier.version.splitToSequence(".").map { it.toInt() }.toList()
        val majorVersion = versions[0]
        if (majorVersion < 5) {
            return false
        }
        val minorVersion = versions[1]
        return majorVersion != 5 || minorVersion >= 14
    }

    abstract class JavaAgentArgumentProvider : CommandLineArgumentProvider {

        @get:Classpath
        abstract val classpath: ConfigurableFileCollection

        override fun asArguments() = listOf("-javaagent:${classpath.singleFile.absolutePath}")

    }
}
