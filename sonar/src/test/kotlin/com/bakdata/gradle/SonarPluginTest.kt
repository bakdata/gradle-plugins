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

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Condition
import org.assertj.core.api.SoftAssertions
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.project.DefaultProject
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import java.util.function.Consumer

internal class SonarPluginTest {
    fun taskWithName(name: String): Condition<Task> = Condition({ it.name == name }, "Task with name $name")

    fun Project.evaluate() {
        (this as DefaultProject).evaluate()
    }

    @Test
    fun testSingleModuleProjectWithJavaFirst() {
        val project = ProjectBuilder.builder().build()

        Assertions.assertThatCode {
            project.pluginManager.apply("java")
            project.pluginManager.apply("com.bakdata.sonar")
            project.evaluate()
        }.doesNotThrowAnyException()

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(project.collectTasks())
                    .haveExactly(1, taskWithName("jacocoTestReport"))
                    .haveExactly(1, taskWithName("sonarqube"))
        }
    }

    @Test
    fun testSingleModuleProjectWithJavaLast() {
        val project = ProjectBuilder.builder().build()

        Assertions.assertThatCode {
            project.pluginManager.apply("com.bakdata.sonar")
            project.pluginManager.apply("java")
            project.evaluate()
        }.doesNotThrowAnyException()

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(project.collectTasks())
                    .haveExactly(1, taskWithName("jacocoTestReport"))
                    .haveExactly(1, taskWithName("sonarqube"))
        }
    }

    @Test
    fun testMultiModuleProjectWithJavaLast() {
        val parent = ProjectBuilder.builder().withName("parent").build()
        val child1 = ProjectBuilder.builder().withName("child1").withParent(parent).build()
        val child2 = ProjectBuilder.builder().withName("child2").withParent(parent).build()
        val children = listOf(child1, child2)

        Assertions.assertThatCode {
            parent.pluginManager.apply("com.bakdata.sonar")
            children.forEach { it.pluginManager.apply("java") }
            parent.evaluate()
        }.doesNotThrowAnyException()

        SoftAssertions.assertSoftly { softly ->
            children.forEach { child ->
                softly.assertThat(child.tasks)
                        .haveExactly(1, taskWithName("jacocoTestReport"))
                        .haveExactly(0, taskWithName("sonarqube"))
            }
        }

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(parent.collectTasks())
                    .haveExactly(0, taskWithName("jacocoTestReport"))
                    .haveExactly(1, taskWithName("sonarqube"))
        }
    }

    @Test
    fun testMultiModuleProjectWithJavaFirst() {
        val parent = ProjectBuilder.builder().withName("parent").build()
        val child1 = ProjectBuilder.builder().withName("child1").withParent(parent).build()
        val child2 = ProjectBuilder.builder().withName("child2").withParent(parent).build()
        val children = listOf(child1, child2)

        Assertions.assertThatCode {
            children.forEach { it.pluginManager.apply("java") }
            parent.pluginManager.apply("com.bakdata.sonar")
            parent.evaluate()
        }.doesNotThrowAnyException()

        SoftAssertions.assertSoftly { softly ->
            children.forEach { child ->
                softly.assertThat(child.tasks)
                        .haveExactly(1, taskWithName("jacocoTestReport"))
                        .haveExactly(0, taskWithName("sonarqube"))
            }
        }

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(parent.collectTasks())
                    .haveExactly(0, taskWithName("jacocoTestReport"))
                    .haveExactly(1, taskWithName("sonarqube"))
        }
    }

    @Test
    fun testMultiModuleProjectWithJavaRootProjectWithJavaLast() {
        val parent = ProjectBuilder.builder().withName("parent").build()
        val child = ProjectBuilder.builder().withName("child").withParent(parent).build()
        val children = listOf(child)

        Assertions.assertThatCode {
            parent.pluginManager.apply("com.bakdata.sonar")
            parent.pluginManager.apply("java")
            children.forEach { it.pluginManager.apply("java") }
            parent.evaluate()
        }.doesNotThrowAnyException()

        SoftAssertions.assertSoftly { softly ->
            children.forEach { child ->
                softly.assertThat(child.tasks)
                    .haveExactly(1, taskWithName("jacocoTestReport"))
                    .haveExactly(0, taskWithName("sonarqube"))
            }
        }

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(parent.collectTasks())
                .haveExactly(1, taskWithName("jacocoTestReport"))
                .haveExactly(1, taskWithName("sonarqube"))
        }
    }

    @Test
    fun testMultiModuleProjectWithJavaRootProjectWithJavaFirst() {
        val parent = ProjectBuilder.builder().withName("parent").build()
        val child = ProjectBuilder.builder().withName("child").withParent(parent).build()
        val children = listOf(child)

        Assertions.assertThatCode {
            parent.pluginManager.apply("java")
            children.forEach { it.pluginManager.apply("java") }
            parent.pluginManager.apply("com.bakdata.sonar")
            parent.evaluate()
        }.doesNotThrowAnyException()

        SoftAssertions.assertSoftly { softly ->
            children.forEach { child ->
                softly.assertThat(child.tasks)
                    .haveExactly(1, taskWithName("jacocoTestReport"))
                    .haveExactly(0, taskWithName("sonarqube"))
            }
        }

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(parent.collectTasks())
                .haveExactly(1, taskWithName("jacocoTestReport"))
                .haveExactly(1, taskWithName("sonarqube"))
        }
    }

    @Test
    fun testWrongApplicationInMultiModuleProject() {
        val parent = ProjectBuilder.builder().withName("parent").build()
        val child1 = ProjectBuilder.builder().withName("child1").withParent(parent).build()

        assertThatThrownBy { child1.pluginManager.apply("com.bakdata.sonar") }
            .satisfies(Consumer { assertThat(it.cause).hasMessageContaining("top-level project") }) // TODO remove explicit Consumer once https://github.com/assertj/assertj/issues/2357 is resolved
    }

    private fun Project.collectTasks(): List<Task> = try {
        tasks.toList()
    } catch (e: GradleException) {
        // FIXME bug since Gradle 7.3 https://github.com/gradle/gradle/issues/20301
        if (e.message.equals("Could not create task ':init'.")) tasks.toList() else throw e
    }
}
