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

import org.assertj.core.api.Condition
import org.assertj.core.api.SoftAssertions
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

internal class SonarPluginIT {
    private fun taskWithPathAndOutcome(path: String, outcome: TaskOutcome):
            Condition<BuildTask> = Condition({ it.path == path && it.outcome == outcome }, "Task $path=$outcome")

    private fun GradleRunner.withProjectPluginClassPath(): GradleRunner {
        val classpath = System.getProperty("java.class.path")
        return withPluginClasspath(classpath.split(":").map { File(it) })
    }

    @Test
    fun testSingleModuleProject(@TempDir testProjectDir: Path) {
        Files.writeString(testProjectDir.resolve("build.gradle.kts"), """
            plugins {
                id("com.bakdata.sonar")
                java
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.0")
                testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.0")
            }
            tasks.withType<Test> {
                useJUnitPlatform()
            }
        """.trimIndent())
        Files.createDirectories(testProjectDir.resolve("src/test/java/"))
        Files.copy(SonarPluginIT::class.java.getResourceAsStream("/DemoTest.java"),
                testProjectDir.resolve("src/test/java/DemoTest.java"))

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("sonarqube", "-Dsonar.scanner.dumpToFile=${testProjectDir.resolve("dump")}")
                .withProjectPluginClassPath()
                .build()

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(result.tasks)
                    .haveExactly(1, taskWithPathAndOutcome(":compileTestJava", TaskOutcome.SUCCESS))
                    .haveExactly(1, taskWithPathAndOutcome(":test", TaskOutcome.SUCCESS))
                    .haveExactly(1, taskWithPathAndOutcome(":jacocoTestReport", TaskOutcome.SUCCESS))
                    .haveExactly(1, taskWithPathAndOutcome(":sonarqube", TaskOutcome.SUCCESS))
        }
    }

    @Test
    fun testMultiModuleProject(@TempDir testProjectDir: Path) {
        Files.writeString(testProjectDir.resolve("build.gradle.kts"), """
            plugins {
                id("com.bakdata.sonar")
            }
            subprojects {
                apply(plugin = "java")
                repositories {
                    mavenCentral()
                }
                dependencies {
                    "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.3.0")
                    "testImplementation"("org.junit.jupiter:junit-jupiter-api:5.3.0")
                }
                tasks.withType<Test> {
                    useJUnitPlatform()
                }
            }
        """.trimIndent())

        val children = listOf("child1", "child2")
        Files.writeString(testProjectDir.resolve("settings.gradle"),
                """include ${children.joinToString(",") { "'$it'" }}""")
        children.forEach { child ->
            Files.createDirectories(testProjectDir.resolve("$child/src/test/java/"))
            Files.copy(SonarPluginIT::class.java.getResourceAsStream("/DemoTest.java"),
                    testProjectDir.resolve("$child/src/test/java/DemoTest.java"))
        }

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("sonarqube", "-Dsonar.scanner.dumpToFile=${testProjectDir.resolve("dump")}", "--stacktrace", "--info")
                .withProjectPluginClassPath()
                .build()

        SoftAssertions.assertSoftly { softly ->
            children.forEach { child ->
                softly.assertThat(result.tasks)
                        .haveExactly(1, taskWithPathAndOutcome(":$child:compileTestJava", TaskOutcome.SUCCESS))
                        .haveExactly(1, taskWithPathAndOutcome(":$child:test", TaskOutcome.SUCCESS))
                        .haveExactly(1, taskWithPathAndOutcome(":$child:jacocoTestReport", TaskOutcome.SUCCESS))
            }
            softly.assertThat(result.tasks)
                    .haveExactly(0, taskWithPathAndOutcome(":jacocoTestReport", TaskOutcome.SUCCESS))
                    .haveExactly(1, taskWithPathAndOutcome(":sonarqube", TaskOutcome.SUCCESS))
        }
    }

    @Test
    fun testMultiModuleProjectWithRootJavaProject(@TempDir testProjectDir: Path) {
        Files.writeString(
            testProjectDir.resolve("build.gradle.kts"), """
            plugins {
                id("com.bakdata.sonar")
            }
            allprojects {
                apply(plugin = "java")
                repositories {
                    mavenCentral()
                }
                dependencies {
                    "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.3.0")
                    "testImplementation"("org.junit.jupiter:junit-jupiter-api:5.3.0")
                }
                tasks.withType<Test> {
                    useJUnitPlatform()
                }
            }
        """.trimIndent()
        )

        val children = listOf("child")
        Files.writeString(
            testProjectDir.resolve("settings.gradle"),
            """include ${children.joinToString(",") { "'$it'" }}"""
        )
        children.forEach { child ->
            Files.createDirectories(testProjectDir.resolve("$child/src/test/java/"))
            Files.copy(
                SonarPluginIT::class.java.getResourceAsStream("/DemoTest.java"),
                testProjectDir.resolve("$child/src/test/java/DemoTest.java")
            )
        }
        Files.createDirectories(testProjectDir.resolve("src/test/java/"))
        Files.copy(
            SonarPluginIT::class.java.getResourceAsStream("/DemoTest.java"),
            testProjectDir.resolve("src/test/java/DemoTest.java")
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withArguments(
                "sonarqube",
                "-Dsonar.scanner.dumpToFile=${testProjectDir.resolve("dump")}",
                "--stacktrace",
                "--info"
            )
            .withProjectPluginClassPath()
            .build()

        SoftAssertions.assertSoftly { softly ->
            children.forEach { child ->
                softly.assertThat(result.tasks)
                    .haveExactly(1, taskWithPathAndOutcome(":$child:compileTestJava", TaskOutcome.SUCCESS))
                    .haveExactly(1, taskWithPathAndOutcome(":$child:test", TaskOutcome.SUCCESS))
                    .haveExactly(1, taskWithPathAndOutcome(":$child:jacocoTestReport", TaskOutcome.SUCCESS))
            }
            softly.assertThat(result.tasks)
                .haveExactly(1, taskWithPathAndOutcome(":compileTestJava", TaskOutcome.SUCCESS))
                .haveExactly(1, taskWithPathAndOutcome(":test", TaskOutcome.SUCCESS))
                .haveExactly(0, taskWithPathAndOutcome(":jacocoTestReport", TaskOutcome.SUCCESS))
                .haveExactly(1, taskWithPathAndOutcome(":sonarqube", TaskOutcome.SUCCESS))
        }
    }
}
