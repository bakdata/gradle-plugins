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

import com.google.cloud.tools.jib.gradle.JibExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import java.util.*

class JibPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        with(project) {
            apply(plugin = "com.google.cloud.tools.jib")

            val extension =
                extensions.create(
                    BakdataJibExtension.EXTENSION_NAME,
                    BakdataJibExtension::class,
                    project,
                )

            afterEvaluate {
                val repository = extension.imageRepository.orNull?.let { "$it/" } ?: ""
                val tag = extension.imageTag.orNull?.let { ":$it" } ?: ""
                // The imageString should not contain any uppercase letters
                // https://docs.docker.com/engine/reference/commandline/tag/#extended-description
                val imageString = "${repository}${extension.imageName.get()}${tag}".lowercase(Locale.getDefault())
                configure<JibExtension> {
                    to {
                        image = imageString
                    }
                }
            }
        }
    }
}
