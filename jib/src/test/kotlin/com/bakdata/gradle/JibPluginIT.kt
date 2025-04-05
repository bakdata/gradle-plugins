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

import org.assertj.core.api.SoftAssertions
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

internal class JibPluginIT {

    @Test
    fun testConfigureDefault(@TempDir testProjectDir: Path) {
        Files.writeString(
            testProjectDir.resolve("settings.gradle"),
            """rootProject.name = 'jib-test-project'"""
        )
        Files.writeString(
            testProjectDir.resolve("build.gradle.kts"), """
            plugins {
                java
                id("com.bakdata.jib")
            }

            tasks.create("showJibImage") {
                afterEvaluate {
                    print(jib.to.image)
                }
            }
        """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withArguments("showJibImage")
            .withPluginClasspath()
            .build()

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(result.output)
                .contains("jib-test-project")
        }
    }

    @Test
    fun testConfigureProperties(@TempDir testProjectDir: Path) {
        Files.writeString(
            testProjectDir.resolve("settings.gradle"),
            """rootProject.name = 'jib-test-project'"""
        )
        Files.writeString(
            testProjectDir.resolve("build.gradle.kts"), """
            plugins {
                java
                id("com.bakdata.jib")
            }

            tasks.create("showJibImage") {
                afterEvaluate {
                    print(jib.to.image)
                }
            }
        """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withArguments(
                "showJibImage",
                "-DjibImage.repository=gcr.io/bakdata",
                "-DjibImage.tags=a-tag"
            )
            .withPluginClasspath()
            .build()

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(result.output)
                .contains("gcr.io/bakdata/jib-test-project:a-tag")
        }
    }

    @Test
    fun testConfigure(@TempDir testProjectDir: Path) {
        Files.writeString(
            testProjectDir.resolve("settings.gradle"),
            """rootProject.name = 'jib-test-project'"""
        )
        Files.writeString(
            testProjectDir.resolve("build.gradle.kts"), """
            plugins {
                java
                id("com.bakdata.jib")
            }

            jibImage {
                repository.set("gcr.io/bakdata")
                name.set("jib-image")
                tags.set(listOf("a-tag", "another-tag"))
                tagSuffix.set("extern")
            }

            tasks.create("showJibImage") {
                afterEvaluate {
                    print(jib.to.image)
                }
            }

            tasks.create("showJibTags") {
                afterEvaluate {
                    print(jib.to.tags) // this task somehow appends the additional tags to jib.to.image
                }
            }
        """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withArguments("showJibImage")
            .withPluginClasspath()
            .build()

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(result.output)
                .contains("gcr.io/bakdata/jib-image:a-tag[another-tag, a-tag-extern, another-tag-extern]")
        }
    }
}
