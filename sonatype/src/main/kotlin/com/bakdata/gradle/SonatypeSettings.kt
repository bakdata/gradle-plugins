package com.bakdata.gradle

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPomDeveloperSpec
import java.io.File

import org.gradle.kotlin.dsl.*

open class SonatypeSettings(project: Project) {
     var disallowLocalRelease: Boolean = true
     var osshrJiraUsername: String? = System.getenv("OSSRH_JIRA_USERNAME")
     var osshrJiraPassword: String? = System.getenv("OSSRH_JIRA_PASSWORD")
     var signingKeyId: String? = System.getenv("SIGNING_KEY_ID") // fallback to -Psigning.keyId=...
     var signingSecretKeyRingFile: String? = System.getenv("SIGNING_SECRET_KEY_RING_FILE") // signing.secretKeyRingFile
     var signingPassword: String? = System.getenv("SIGNING_PASSWORD") // signing.password
     var repoName: String = project.name
     var repoUrl: String = "https://github.com/bakdata/${repoName}"
     var description: String? = null
     var developers: Action<in MavenPomDeveloperSpec>? = null

     fun developers(developerSpec: Action<in MavenPomDeveloperSpec>) {
          this.developers = developerSpec
     }
}