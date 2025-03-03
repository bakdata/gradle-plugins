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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPomDeveloperSpec

open class SonatypeSettings(var project: Project) {
     var disallowLocalRelease: Boolean = true
     var osshrUsername: String? = System.getenv("OSSRH_USERNAME")
          get() = field ?: project.findProperty("ossrh.username")?.toString()
     var osshrPassword: String? = System.getenv("OSSRH_PASSWORD")
          get() = field ?: project.findProperty("ossrh.password")?.toString()
     var signingKeyId: String? = System.getenv("SIGNING_KEY_ID")
          get() = field ?: project.findProperty("signing.keyId")?.toString()
     var signingSecretKeyRingFile: String? = System.getenv("SIGNING_SECRET_KEY_RING_FILE")
          get() = field ?: project.findProperty("signing.secretKeyRingFile")?.toString()
     var signingPassword: String? = System.getenv("SIGNING_PASSWORD")
          get() = field ?: project.findProperty("signing.password")?.toString()
     var repoName: String? = null
          get() = field ?: project.rootProject.name
     var repoUrl: String? = null
          get() = field ?: "https://github.com/bakdata/${repoName}"
     var description: String? = null
          get() = field ?: project.description
     var nexusUrl: String? = null
     var snapshotUrl: String? = null
     var developers: Action<in MavenPomDeveloperSpec>? = null
     var connectTimeout: Long = 300
     var clientTimeout: Long = 300
     var allowInsecureProtocol: Boolean = false
     var createPublication: Boolean? = null

     fun developers(developerSpec: Action<in MavenPomDeveloperSpec>) {
          this.developers = developerSpec
     }
}
