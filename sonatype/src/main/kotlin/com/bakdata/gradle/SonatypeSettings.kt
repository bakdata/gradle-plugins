package com.bakdata.gradle

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPomDeveloperSpec
import java.io.File

import org.gradle.kotlin.dsl.*

open class SonatypeSettings(project: Project) {
     var disallowLocalRelease: Boolean = true
     var osshrJiraUsername: String? = System.getenv("OSSRH_JIRA_USERNAME") ?: project.findProperty("ossrh.username")?.toString()
     var osshrJiraPassword: String? = System.getenv("OSSRH_JIRA_PASSWORD") ?: project.findProperty("ossrh.password")?.toString()
     var signingKeyId: String? = System.getenv("SIGNING_KEY_ID") ?: project.findProperty("signing.keyId")?.toString()
     var signingSecretKeyRingFile: String? = System.getenv("SIGNING_SECRET_KEY_RING_FILE") ?: project.findProperty("signing.secretKeyRingFile")?.toString()
     var signingPassword: String? = System.getenv("SIGNING_PASSWORD") ?: project.findProperty("signing.password")?.toString()
     var repoName: String = project.name
     var repoUrl: String = "https://github.com/bakdata/${repoName}"
     var description: String? = null
     var developers: Action<in MavenPomDeveloperSpec>? = null

     fun developers(developerSpec: Action<in MavenPomDeveloperSpec>) {
          this.developers = developerSpec
     }
}