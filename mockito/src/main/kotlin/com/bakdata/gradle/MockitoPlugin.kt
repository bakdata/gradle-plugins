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
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.testing.Test

class MockitoPlugin : Plugin<Project> {
    private val log = Logging.getLogger(MockitoPlugin::class.java)

    override fun apply(project: Project) {
        // according to https://javadoc.io/doc/org.mockito/mockito-core/5.15.2/org/mockito/Mockito.html#0.3
        project.configurations.matching { it.name == "testRuntimeClasspath" }.all {
            val testRuntimeClasspath = this
            val tests = project.tasks.withType(Test::class.java)
            tests.configureEach {
                val resolvedConfiguration = testRuntimeClasspath.resolvedConfiguration
                val resolvedArtifacts = resolvedConfiguration.resolvedArtifacts
                resolvedArtifacts.find { it.moduleVersion.id.module.toString() == "org.mockito:mockito-core" }
                    ?.let { mockito ->
                        tests.forEach {
                            log.info("Configuring Mockito java agent for task ${it.name}")
                            it.jvmArgs("-javaagent:${mockito.file}")
                        }
                    }
            }
        }
    }
}
