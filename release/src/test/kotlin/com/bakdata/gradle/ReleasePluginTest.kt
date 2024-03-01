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

import net.researchgate.release.ReleasePlugin
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Condition
import org.assertj.core.api.SoftAssertions
import org.gradle.api.Project
import org.gradle.api.internal.project.DefaultProject
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

internal class ReleasePluginTest {

    private fun Project.evaluate() {
        (this as DefaultProject).evaluate()
    }

    @Test
    fun testSingleModuleProjectWithJavaFirst() {
        val project = ProjectBuilder.builder().build()

        assertThatCode {
            project.pluginManager.apply("com.bakdata.release")
            project.evaluate()
        }.doesNotThrowAnyException()

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(project.plugins)
                .haveExactly(1, Condition({ it is ReleasePlugin }, "Has release plugin"))
        }
    }

    @Test
    fun testWrongApplicationInMultiModuleProject() {
        val parent = ProjectBuilder.builder().withName("parent").build()
        val child1 = ProjectBuilder.builder().withName("child1").withParent(parent).build()

        assertThatCode { child1.pluginManager.apply("com.bakdata.release") }
            .satisfies { assertThat(it.cause).hasMessageContaining("top-level project") }
    }
}
