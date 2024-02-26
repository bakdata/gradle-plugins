/*
 * The MIT License
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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.assertj.core.api.SoftAssertions
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junitpioneer.jupiter.TempDirectory
import org.junitpioneer.jupiter.TempDirectory.TempDir
import ru.lanwen.wiremock.ext.WiremockResolver
import ru.lanwen.wiremock.ext.WiremockResolver.Wiremock
import java.io.File
import java.nio.file.Files
import java.nio.file.Path


@ExtendWith(TempDirectory::class, WiremockResolver::class)
internal class SonatypePluginIT {
    private fun taskWithPathAndOutcome(path: String, outcome: TaskOutcome):
            Condition<BuildTask> = Condition({ it.path == path && it.outcome == outcome }, "Task $path=$outcome")

    private fun GradleRunner.withProjectPluginClassPath() : GradleRunner {
        val classpath = System.getProperty("java.class.path")
        return withPluginClasspath(classpath.split(":").map { File(it) })
    }

    @Test
    fun testSingleModuleProject(@TempDir testProjectDir: Path, @Wiremock wiremock: WireMockServer) {
        Files.writeString(testProjectDir.resolve("build.gradle.kts"), """
            plugins {
                id("com.bakdata.sonatype")
            }
            apply(plugin = "java")
            group = "$TEST_GROUP"
            version = "$TEST_VERSION"
            configure<com.bakdata.gradle.SonatypeSettings> {
                disallowLocalRelease = false
                osshrUsername = "dummy user"
                osshrPassword = "dummy pw"
                description = "dummy description"
                signingKeyId = "72217EAF"
                signingPassword = "test_password"
                signingSecretKeyRingFile = "${File(SonatypePluginIT::class.java.getResource("/test_secring.gpg").toURI()).absolutePath}"
                developers {
                    developer {
                        name.set("dummy name")
                        id.set("dummy id")
                    }
                }
                nexusUrl = "${wiremock.baseUrl()}"
            }
        """.trimIndent())

        Files.createDirectories(testProjectDir.resolve("src/main/java/"))
        Files.copy(SonatypePluginIT::class.java.getResourceAsStream("/Demo.java"),
                testProjectDir.resolve("src/main/java/Demo.java"))

        mockNexusProtocol(wiremock)

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("publishToSonatype", "closeAndReleaseStagingRepository", "--stacktrace", "--info")
                .withProjectPluginClassPath()
                .build()

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(result.tasks)
                    .haveExactly(1, taskWithPathAndOutcome(":signSonatypePublication", TaskOutcome.SUCCESS))
                    .haveExactly(1, taskWithPathAndOutcome(":publishSonatypePublicationToNexusRepository", TaskOutcome.SUCCESS))
                    .haveExactly(1, taskWithPathAndOutcome(":closeAndReleaseStagingRepository", TaskOutcome.SUCCESS))
        }

        val projectName = testProjectDir.fileName.toString()
        val expectedUploads = listOf(".jar", ".pom", "-javadoc.jar", "-sources.jar", ".module")
                .map { classifier -> "$projectName/$TEST_VERSION/$projectName-$TEST_VERSION$classifier" }
                .flatMap { baseFile -> listOf(baseFile, "$baseFile.asc") }
                .plus("$projectName/maven-metadata.xml")
                .flatMap { file -> listOf(file, "$file.md5", "$file.sha1", "$file.sha256", "$file.sha512") }
        assertThat(getUploadedFilesInGroup(wiremock)).containsExactlyInAnyOrderElementsOf(expectedUploads)
    }

    private fun getUploadedFilesInGroup(wiremock: WireMockServer): List<String> {
        val baseUrl = "/staging/deployByRepositoryId/$STAGING_ID/${TEST_GROUP.replace('.', '/')}/"
        return wiremock.allServeEvents
                .filter { it.request.method == RequestMethod.PUT && it.request.url.startsWith("/staging/deploy") }
                .map { it.request.url.substringAfter(baseUrl) }
    }

    private fun mockNexusProtocol(wiremock: WireMockServer) {
        wiremock.stubFor(get(urlMatching("/staging/profiles"))
                .willReturn(okJson("""{"data": [{"id": $PROFILE_ID, "name": "${TEST_GROUP}"}]}""")))
        wiremock.stubFor(post(urlMatching("/staging/profiles/$PROFILE_ID/start"))
                .willReturn(okJson("""{"data": {"stagedRepositoryId": $STAGING_ID}}""")))
        wiremock.stubFor(get(urlMatching("/staging/deployByRepositoryId/$STAGING_ID/.*/maven-metadata.xml"))
                .willReturn(okXml("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <metadata modelVersion="1.1.0">
                    </metadata>
                """.trimIndent())))
        wiremock.stubFor(put(urlMatching("/staging/deploy.*"))
                .willReturn(ok()))
        wiremock.stubFor(get(urlMatching("/staging/profile_repositories/$PROFILE_ID"))
                .willReturn(okJson("""{"data": [{"type": "open", "repositoryId": "$REPO_ID"}]}""")))
        wiremock.stubFor(post(urlMatching("/staging/bulk/close"))
                .willReturn(okJson("{}")))
        wiremock.stubFor(get(urlMatching("/staging/repository/$STAGING_ID"))
                .willReturn(okJson("""{"type": "closed", "transitioning": false}""")))
        wiremock.stubFor(post(urlMatching("/staging/bulk/promote"))
                .inScenario("promoting").whenScenarioStateIs(STARTED)
                .willReturn(okJson("{}"))
                .willSetStateTo("promoting"))
        wiremock.stubFor(get(urlMatching("/staging/repository/$STAGING_ID"))
                .inScenario("promoting").whenScenarioStateIs("promoting")
                .willReturn(okJson("""{"type": "released", "transitioning": false}""")))
    }

    @Test
    fun testMultiModuleProject(@TempDir testProjectDir: Path, @Wiremock wiremock: WireMockServer) {
        Files.writeString(testProjectDir.resolve("build.gradle.kts"), """
            plugins {
                id("com.bakdata.sonatype")
            }

            configure<com.bakdata.gradle.SonatypeSettings> {
                disallowLocalRelease = false
                osshrUsername = "dummy user"
                osshrPassword = "dummy pw"
                description = "dummy description"
                signingKeyId = "72217EAF"
                signingPassword = "test_password"
                signingSecretKeyRingFile = "${File(SonatypePluginIT::class.java.getResource("/test_secring.gpg").toURI()).absolutePath}"
                developers {
                    developer {
                        name.set("dummy name")
                        id.set("dummy id")
                    }
                }
                nexusUrl = "${wiremock.baseUrl()}"
            }

            subprojects {
                apply(plugin = "java")
                version = "$TEST_VERSION"
                group = "${TEST_GROUP}"
            }
        """.trimIndent())

        val children = listOf("child1", "child2")
        Files.writeString(testProjectDir.resolve("settings.gradle"),
                """include ${children.joinToString(",") { "'$it'" }}""")
        children.forEach { child ->
            Files.createDirectories(testProjectDir.resolve("$child/src/main/java/"))
            Files.copy(SonatypePluginIT::class.java.getResourceAsStream("/Demo.java"),
                    testProjectDir.resolve("$child/src/main/java/Demo.java"))
        }

        mockNexusProtocol(wiremock)

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("publishToSonatype", "closeAndReleaseStagingRepository", "--stacktrace", "--info")
                .withProjectPluginClassPath()
                .build()

        SoftAssertions.assertSoftly { softly ->
            children.forEach { child ->
                softly.assertThat(result.tasks)
                        .haveExactly(1, taskWithPathAndOutcome(":$child:signSonatypePublication", TaskOutcome.SUCCESS))
                        .haveExactly(1, taskWithPathAndOutcome(":$child:publishSonatypePublicationToNexusRepository", TaskOutcome.SUCCESS))
            }
            softly.assertThat(result.tasks)
                    .haveExactly(0, taskWithPathAndOutcome(":signSonatypePublication", TaskOutcome.SUCCESS))
                    .haveExactly(0, taskWithPathAndOutcome(":publishSonatypePublicationToNexusRepository", TaskOutcome.SUCCESS))
                    .haveExactly(1, taskWithPathAndOutcome(":closeAndReleaseStagingRepository", TaskOutcome.SUCCESS))
        }

        val expectedUploads = listOf(".jar", ".pom", "-javadoc.jar", "-sources.jar", ".module")
                .flatMap { classifier -> children.map { child -> "$child/$TEST_VERSION/$child-$TEST_VERSION$classifier" } }
                .flatMap { baseFile -> listOf(baseFile, "$baseFile.asc") }
                .plus(children.map { child -> "$child/maven-metadata.xml" })
                .flatMap { file -> listOf(file, "$file.md5", "$file.sha1", "$file.sha256", "$file.sha512") }
        assertThat(getUploadedFilesInGroup(wiremock)).containsExactlyInAnyOrderElementsOf(expectedUploads)
    }

    companion object {
        private const val TEST_GROUP: String = "com.bakdata"

        private const val PROFILE_ID: String = "8412378836ed9c"

        private const val STAGING_ID: Int = 5

        private const val REPO_ID: Int = 9

        private const val TEST_VERSION: String = "1.0.0"
    }
}
