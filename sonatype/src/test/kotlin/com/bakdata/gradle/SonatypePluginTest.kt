/*
 * MIT License
 *
 * Copyright (c) 2024 bakdata GmbH
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
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.project.DefaultProject
import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.util.function.Consumer


internal class SonatypePluginTest {
    fun Project.evaluate() {
        (this as DefaultProject).evaluate()
    }

    fun taskWithName(name: String): Condition<Task> = Condition({ it.name == name }, "Task with name $name")

    @Test
    fun testSingleModuleProject() {
        val project = ProjectBuilder.builder().build()

        Assertions.assertThatCode {
            project.apply(plugin = "com.bakdata.sonatype")
            project.apply(plugin = "java")

            File(project.projectDir, "src/main/java/").mkdirs()
            Files.copy(SonatypePluginTest::class.java.getResourceAsStream("/Demo.java"),
                    File(project.projectDir, "src/main/java/Demo.java").toPath())

            project.evaluate()
        }.doesNotThrowAnyException()

        assertSoftly { softly ->
            softly.assertThat(project.collectTasks())
                    .haveExactly(1, taskWithName("signSonatypePublication"))
                    .haveExactly(1, taskWithName("publish"))
                    .haveExactly(1, taskWithName("publishToNexus"))
                    .haveExactly(1, taskWithName("closeAndReleaseStagingRepository"))
        }
    }

    @Test
    fun testMultiModuleProject() {
        val parent = ProjectBuilder.builder().withName("parent").build()
        val child1 = ProjectBuilder.builder().withName("child1").withParent(parent).build()
        val child2 = ProjectBuilder.builder().withName("child2").withParent(parent).build()
        val children = listOf(child1, child2)

        Assertions.assertThatCode {
            parent.apply(plugin = "com.bakdata.sonatype")

            children.forEach {
                it.apply(plugin = "java")
                File(it.projectDir, "src/main/java/").mkdirs()
                Files.copy(SonatypePluginTest::class.java.getResourceAsStream("/Demo.java"),
                        File(it.projectDir, "src/main/java/Demo.java").toPath())
            }

            parent.evaluate()
            children.forEach { it.evaluate() }
        }.doesNotThrowAnyException()

        assertSoftly { softly ->
            children.forEach { child ->
                softly.assertThat(child.tasks)
                        .haveExactly(1, taskWithName("signSonatypePublication"))
                        .haveExactly(1, taskWithName("publish"))
                        .haveExactly(1, taskWithName("publishToNexus"))
                        .haveExactly(0, taskWithName("closeAndReleaseStagingRepository"))
            }
        }

        assertSoftly { softly ->
            softly.assertThat(parent.collectTasks())
                    .haveExactly(0, taskWithName("signSonatypePublication"))
                    .haveExactly(0, taskWithName("publish"))
                    .haveExactly(0, taskWithName("publishToNexus"))
                    .haveExactly(1, taskWithName("closeAndReleaseStagingRepository"))
        }
    }

    @Test
    fun testWrongApplicationInMultiModuleProject() {
        val parent = ProjectBuilder.builder().withName("parent").build()
        val child1 = ProjectBuilder.builder().withName("child1").withParent(parent).build()

        Assertions.assertThatThrownBy { child1.apply(plugin = "com.bakdata.sonatype") }
            .satisfies(Consumer { Assertions.assertThat(it.cause).hasMessageContaining("top-level project") })
    }

    private fun Project.collectTasks(): List<Task> = try {
        tasks.toList()
    } catch (e: GradleException) {
        // FIXME bug since Gradle 7.3 https://github.com/gradle/gradle/issues/20301
        if (e.message.equals("Could not create task ':init'.")) tasks.toList() else throw e
    }

}

