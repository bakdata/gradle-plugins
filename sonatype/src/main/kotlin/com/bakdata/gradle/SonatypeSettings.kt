package com.bakdata.gradle

import au.com.console.kassava.kotlinToString
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPomDeveloperSpec
import kotlin.reflect.full.memberProperties

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

     fun developers(developerSpec: Action<in MavenPomDeveloperSpec>) {
          this.developers = developerSpec
     }

     override fun toString() = kotlinToString(properties = SonatypeSettings::class.memberProperties.toTypedArray())
}