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

import net.researchgate.release.ReleaseExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.hildan.github.changelog.plugin.GitHubChangelogExtension

class ReleasePlugin : Plugin<Project> {

    companion object {
        const val DISABLE_PUSH_TO_REMOTE = "release.disablePushToRemote"
        const val REQUIRE_BRANCH = "release.requireBranch"
        const val GITHUB_REPOSITORY = "changelog.githubRepository"
        const val FUTURE_VERSION_TAG = "changelog.futureVersionTag"
        const val SINCE_TAG = "changelog.sinceTag"
    }

    override fun apply(rootProject: Project) {
        if (rootProject.parent != null) {
            throw GradleException("Apply this plugin only to the top-level project.")
        }

        with(rootProject) {
            apply(plugin = "net.researchgate.release")

            val disablePushToRemote: String? = project.findProperty(DISABLE_PUSH_TO_REMOTE)?.toString()
            val branch: String? = project.findProperty(REQUIRE_BRANCH)?.toString()
            configure<ReleaseExtension> {
                git {
                    if (disablePushToRemote?.toBoolean() == true) {
                        pushToRemote.set(false)
                    }
                    branch?.also {
                        requireBranch.set(it)
                    }
                }
            }

            apply(plugin = "org.hildan.github.changelog")

            configure<GitHubChangelogExtension> {
                project.findProperty(GITHUB_REPOSITORY)?.toString()?.also {
                    githubRepository = it
                }
                project.findProperty(FUTURE_VERSION_TAG)?.toString()?.also {
                    futureVersionTag = it
                }
                project.findProperty(SINCE_TAG)?.toString()?.also {
                    sinceTag = it
                }
            }
        }
    }
}
