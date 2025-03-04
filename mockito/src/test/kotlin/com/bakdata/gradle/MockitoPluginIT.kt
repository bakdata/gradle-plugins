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
import java.nio.file.Files
import java.nio.file.Path

internal class MockitoPluginIT {
    private fun taskWithPathAndOutcome(path: String, outcome: TaskOutcome):
            Condition<BuildTask> = Condition({ it.path == path && it.outcome == outcome }, "Task $path=$outcome")

    @Test
    fun shouldRegisterAgent(@TempDir testProjectDir: Path) {
        Files.writeString(
            testProjectDir.resolve("build.gradle.kts"), """
            plugins {
                java
                id("com.bakdata.mockito")
            }
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
                testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
                testImplementation("org.mockito:mockito-core:5.15.2")
            }
            tasks.withType<Test> {
                useJUnitPlatform()
            }
        """.trimIndent()
        )
        Files.createDirectories(testProjectDir.resolve("src/test/java/"))
        Files.copy(
            MockitoPluginIT::class.java.getResourceAsStream("/DemoTest.java")!!,
            testProjectDir.resolve("src/test/java/DemoTest.java")
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withArguments(
                "test",
                "--info"
            )
            .withPluginClasspath()
            .build()

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(result.tasks)
                .haveExactly(1, taskWithPathAndOutcome(":test", TaskOutcome.SUCCESS))
            softly.assertThat(result.output)
                .doesNotContain("Please add Mockito as an agent to your build")
        }
    }
}
