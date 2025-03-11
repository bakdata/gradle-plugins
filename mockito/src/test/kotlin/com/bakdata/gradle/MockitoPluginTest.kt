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

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.SoftAssertions
import org.gradle.api.Project
import org.gradle.api.internal.project.DefaultProject
import org.gradle.kotlin.dsl.getByName
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

internal class MockitoPluginTest {

    private fun Project.evaluate() {
        (this as DefaultProject).evaluate()
    }

    @Test
    fun shouldApplyWithJavaFirst() {
        val project = ProjectBuilder.builder().build()

        assertThatCode {
            project.pluginManager.apply("java")
            project.pluginManager.apply("com.bakdata.mockito")
            project.repositories.mavenCentral()
            project.dependencies.add("testImplementation", "org.mockito:mockito-core:5.15.2")
            project.evaluate()
        }.doesNotThrowAnyException()

        SoftAssertions.assertSoftly { softly ->
            val test = project.tasks.getByName("test", org.gradle.api.tasks.testing.Test::class)
            val configureMockitoAgent = project.tasks.named("configureMockitoAgent")
            softly.assertThat(test.dependsOn)
                .contains(configureMockitoAgent)
            softly.assertThat(test.jvmArgumentProviders)
                .noneSatisfy {
                    softly.assertThat(it.asArguments())
                        .anySatisfy {
                            softly.assertThat(it)
                                .contains("-javaagent:")
                                .contains("mockito-core-5.15.2.jar")
                        }
                }
            configureMockitoAgent.get() // force configuration of test
            softly.assertThat(test.jvmArgumentProviders)
                .anySatisfy {
                    softly.assertThat(it.asArguments())
                        .anySatisfy {
                            softly.assertThat(it)
                                .contains("-javaagent:")
                                .contains("mockito-core-5.15.2.jar")
                        }
                }
        }
    }

    @Test
    fun shouldApplyWithJavaLast() {
        val project = ProjectBuilder.builder().build()

        assertThatCode {
            project.pluginManager.apply("com.bakdata.mockito")
            project.pluginManager.apply("java")
            project.repositories.mavenCentral()
            project.dependencies.add("testImplementation", "org.mockito:mockito-core:5.15.2")
            project.evaluate()
        }.doesNotThrowAnyException()

        SoftAssertions.assertSoftly { softly ->
            val test = project.tasks.getByName("test", org.gradle.api.tasks.testing.Test::class)
            val configureMockitoAgent = project.tasks.named("configureMockitoAgent")
            softly.assertThat(test.dependsOn)
                .contains(configureMockitoAgent)
            softly.assertThat(test.jvmArgumentProviders)
                .noneSatisfy {
                    softly.assertThat(it.asArguments())
                        .anySatisfy {
                            softly.assertThat(it)
                                .contains("-javaagent:")
                                .contains("mockito-core-5.15.2.jar")
                        }
                }
            configureMockitoAgent.get() // force configuration of test
            softly.assertThat(test.jvmArgumentProviders)
                .anySatisfy {
                    softly.assertThat(it.asArguments())
                        .anySatisfy {
                            softly.assertThat(it)
                                .contains("-javaagent:")
                                .contains("mockito-core-5.15.2.jar")
                        }
                }
        }
    }

    @Test
    fun shouldNotAddJavaAgentWithMockito5_13() {
        val project = ProjectBuilder.builder().build()

        assertThatCode {
            project.pluginManager.apply("java")
            project.pluginManager.apply("com.bakdata.mockito")
            project.repositories.mavenCentral()
            project.dependencies.add("testImplementation", "org.mockito:mockito-core:5.13.0")
            project.evaluate()
        }.doesNotThrowAnyException()

        SoftAssertions.assertSoftly { softly ->
            val test = project.tasks.getByName("test", org.gradle.api.tasks.testing.Test::class)
            val configureMockitoAgent = project.tasks.named("configureMockitoAgent")
            softly.assertThat(test.dependsOn)
                .contains(configureMockitoAgent)
            configureMockitoAgent.get() // force configuration of test
            softly.assertThat(test.jvmArgs)
                .noneSatisfy {
                    softly.assertThat(it)
                        .contains("-javaagent:")
                        .contains("mockito-core-5.13.0.jar")
                }
        }
    }
}
