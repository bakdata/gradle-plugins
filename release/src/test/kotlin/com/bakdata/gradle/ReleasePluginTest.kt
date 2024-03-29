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

import com.bakdata.gradle.ReleasePlugin.Companion.DISABLE_PUSH_TO_REMOTE
import com.bakdata.gradle.ReleasePlugin.Companion.FUTURE_VERSION_TAG
import com.bakdata.gradle.ReleasePlugin.Companion.GITHUB_REPOSITORY
import com.bakdata.gradle.ReleasePlugin.Companion.REQUIRE_BRANCH
import com.bakdata.gradle.ReleasePlugin.Companion.SINCE_TAG
import net.researchgate.release.ReleaseExtension
import net.researchgate.release.ReleasePlugin
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Condition
import org.assertj.core.api.SoftAssertions
import org.gradle.api.Project
import org.gradle.api.internal.project.DefaultProject
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.findByType
import org.gradle.testfixtures.ProjectBuilder
import org.hildan.github.changelog.plugin.GitHubChangelogExtension
import org.hildan.github.changelog.plugin.GitHubChangelogPlugin
import org.junit.jupiter.api.Test
import java.util.function.Consumer

internal class ReleasePluginTest {

    private fun Project.evaluate() {
        (this as DefaultProject).evaluate()
    }

    @Test
    fun testSingleModuleProject() {
        val project = ProjectBuilder.builder().build()

        assertThatCode {
            project.pluginManager.apply("com.bakdata.release")
            project.evaluate()
        }.doesNotThrowAnyException()

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(project.plugins)
                .haveExactly(1, Condition({ it is ReleasePlugin }, "Has release plugin"))
                .haveExactly(1, Condition({ it is GitHubChangelogPlugin }, "Has changelog plugin"))
            softly.assertThat(project.extensions.findByType<ReleaseExtension>()?.git)
                .satisfies(Consumer { // TODO remove explicit Consumer once https://github.com/assertj/assertj/issues/2357 is resolved
                    softly.assertThat(it?.pushToRemote?.get()).isEqualTo("origin")
                    softly.assertThat(it?.requireBranch?.get()).isEqualTo("main")
                })
            softly.assertThat(project.extensions.findByType<GitHubChangelogExtension>())
                .satisfies(Consumer { // TODO remove explicit Consumer once https://github.com/assertj/assertj/issues/2357 is resolved
                    softly.assertThat(it?.githubRepository).isNull()
                    softly.assertThat(it?.futureVersionTag).isNull()
                    softly.assertThat(it?.sinceTag).isNull()
                })
        }
    }

    @Test
    fun testDisablePushToRemote() {
        val project = ProjectBuilder.builder().build()

        assertThatCode {
            project.extra.set(DISABLE_PUSH_TO_REMOTE, "false")
            project.pluginManager.apply("com.bakdata.release")
            project.evaluate()
        }.doesNotThrowAnyException()

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(project.extensions.findByType<ReleaseExtension>()?.git?.pushToRemote?.get())
                .isEqualTo("origin")
        }
    }

    @Test
    fun testEnablePushToRemote() {
        val project = ProjectBuilder.builder().build()

        assertThatCode {
            project.extra.set(DISABLE_PUSH_TO_REMOTE, "true")
            project.pluginManager.apply("com.bakdata.release")
            project.evaluate()
        }.doesNotThrowAnyException()

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(project.extensions.findByType<ReleaseExtension>()?.git?.pushToRemote?.get())
                .isEqualTo(false)
        }
    }

    @Test
    fun testRequireBranch() {
        val project = ProjectBuilder.builder().build()

        assertThatCode {
            project.extra.set(REQUIRE_BRANCH, "my-branch")
            project.pluginManager.apply("com.bakdata.release")
            project.evaluate()
        }.doesNotThrowAnyException()

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(project.extensions.findByType<ReleaseExtension>()?.git?.requireBranch?.get())
                .isEqualTo("my-branch")
        }
    }

    @Test
    fun testRepository() {
        val project = ProjectBuilder.builder().build()

        assertThatCode {
            project.extra.set(GITHUB_REPOSITORY, "my-repo")
            project.pluginManager.apply("com.bakdata.release")
            project.evaluate()
        }.doesNotThrowAnyException()

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(project.extensions.findByType<GitHubChangelogExtension>()?.githubRepository)
                .isEqualTo("my-repo")
        }
    }

    @Test
    fun testFutureVersionTag() {
        val project = ProjectBuilder.builder().build()

        assertThatCode {
            project.extra.set(FUTURE_VERSION_TAG, "my-tag")
            project.pluginManager.apply("com.bakdata.release")
            project.evaluate()
        }.doesNotThrowAnyException()

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(project.extensions.findByType<GitHubChangelogExtension>()?.futureVersionTag)
                .isEqualTo("my-tag")
        }
    }

    @Test
    fun testSinceTag() {
        val project = ProjectBuilder.builder().build()

        assertThatCode {
            project.extra.set(SINCE_TAG, "my-tag")
            project.pluginManager.apply("com.bakdata.release")
            project.evaluate()
        }.doesNotThrowAnyException()

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(project.extensions.findByType<GitHubChangelogExtension>()?.sinceTag)
                .isEqualTo("my-tag")
        }
    }

    @Test
    fun testWrongApplicationInMultiModuleProject() {
        val parent = ProjectBuilder.builder().withName("parent").build()
        val child1 = ProjectBuilder.builder().withName("child1").withParent(parent).build()

        assertThatThrownBy { child1.pluginManager.apply("com.bakdata.release") }
            .satisfies(Consumer { assertThat(it.cause).hasMessageContaining("top-level project") }) // TODO remove explicit Consumer once https://github.com/assertj/assertj/issues/2357 is resolved
    }
}
