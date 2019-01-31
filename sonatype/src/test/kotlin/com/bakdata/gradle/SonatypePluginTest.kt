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

import org.assertj.core.api.Assertions
import org.assertj.core.api.Condition
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.project.DefaultProject
import org.gradle.kotlin.dsl.configure
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KMutableProperty1


internal class SonatypePluginTest {
    fun Project.configureTestSettings() {
        configure<SonatypeSettings> {
            osshrJiraUsername = "dummy user"
            osshrJiraPassword = "dummy pw"
            description = "dummy description"
            signingKeyId = "dummy signing key"
            signingPassword = "dummy signing password"
            signingSecretKeyRingFile = "dummy signing key file"
            developers {
                developer {
                    name.set("dummy name")
                    id.set("dummy id")
                }
            }
        }
    }

    fun Project.evaluate() {
        (this as DefaultProject).evaluate()
    }

    fun taskWithName(name: String): Condition<Task> = Condition({ it.name == name }, "Task with name $name")

    @Test
    fun testSingleModuleProject() {
        val project = ProjectBuilder.builder().build()

        Assertions.assertThatCode {
            project.pluginManager.apply("com.bakdata.sonatype")
            project.configureTestSettings()
            project.evaluate()
        }.doesNotThrowAnyException()

        assertSoftly { softly ->
            softly.assertThat(project.tasks)
                    .haveExactly(1, taskWithName("signMavenPublication"))
                    .haveExactly(1, taskWithName("publish"))
                    .haveExactly(1, taskWithName("publishToNexus"))
                    .haveExactly(1, taskWithName("closeAndReleaseRepository"))
        }
    }

    @Test
    fun testMultiModuleProject() {
        val parent = ProjectBuilder.builder().withName("parent").build()
        val child1 = ProjectBuilder.builder().withName("child1").withParent(parent).build()
        val child2 = ProjectBuilder.builder().withName("child2").withParent(parent).build()
        val children = listOf(child1, child2)

        Assertions.assertThatCode {
            parent.pluginManager.apply("com.bakdata.sonatype")
            parent.configureTestSettings()
            parent.evaluate()
        }.doesNotThrowAnyException()

        assertSoftly { softly ->
            children.forEach { child ->
                softly.assertThat(child.tasks)
                        .haveExactly(1, taskWithName("signMavenPublication"))
                        .haveExactly(1, taskWithName("publish"))
                        .haveExactly(1, taskWithName("publishToNexus"))
                        .haveExactly(0, taskWithName("closeAndReleaseRepository"))
            }
        }

        assertSoftly { softly ->
            softly.assertThat(parent.tasks)
                    .haveExactly(0, taskWithName("signMavenPublication"))
                    .haveExactly(0, taskWithName("publish"))
                    .haveExactly(1, taskWithName("publishToNexus"))
                    .haveExactly(1, taskWithName("closeAndReleaseRepository"))
        }
    }

    @Test
    fun testWrongApplicationInMultiModuleProject() {
        val parent = ProjectBuilder.builder().withName("parent").build()
        val child1 = ProjectBuilder.builder().withName("child1").withParent(parent).build()

        Assertions.assertThatCode { child1.pluginManager.apply("com.bakdata.sonatype") }
                .satisfies { Assertions.assertThat(it.cause?.message).contains("top-level project") }
    }

    companion object {
        @Suppress("unused")
        @JvmStatic
        fun requiredProperties(): List<KMutableProperty1<SonatypeSettings, out Any?>> =
                listOf(SonatypeSettings::osshrJiraUsername,
                        SonatypeSettings::osshrJiraPassword,
                        SonatypeSettings::signingKeyId,
                        SonatypeSettings::signingPassword,
                        SonatypeSettings::signingSecretKeyRingFile,
                        SonatypeSettings::description,
                        SonatypeSettings::developers)
    }

    @ParameterizedTest
    @MethodSource("requiredProperties")
    fun failsWhenMissingRequiredInformation(property: KMutableProperty1<SonatypeSettings, Any?>) {
        val project = ProjectBuilder.builder().build()

        Assertions.assertThatCode {
            project.pluginManager.apply("com.bakdata.sonatype")
            project.configureTestSettings()
            project.configure<SonatypeSettings> {
                property.set(this, null)
            }
            project.evaluate()
        }.isNotNull()
                .satisfies { Assertions.assertThat(it.cause?.message).contains("sonatype.${property.name}") }
    }
}

