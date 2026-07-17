import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "2.3.20"
  id("org.jetbrains.intellij.platform") version "2.6.0"
  // Single-sources the plugin change-notes from CHANGELOG.md (see the changelog
  // block and patchPluginXml below) and provides the markdownToHTML helper.
  id("org.jetbrains.changelog") version "2.5.0"
}

group = "net.nightvision"
version = "2.2.1"


repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  intellijPlatform {
    intellijIdeaCommunity("2024.3.5")
    bundledPlugin("com.intellij.java")
    pluginVerifier()
  }
  testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
  pluginVerification {
    ides.ides(listOf("IC-2023.3.8", "IC-2024.3.5"))
  }
}

// CHANGELOG.md is the single source of truth for release notes. patchPluginXml
// (below) renders the current version's section into the plugin change-notes,
// and patchChangelog rolls the Unreleased section into a dated version section
// at release time.
changelog {
  version.set(project.version.toString())
  repositoryUrl.set("https://github.com/nvsecurity/nightvision-intellij")
}

tasks.withType<KotlinJvmCompile> {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
  }
}

tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
  }

  patchPluginXml {
    sinceBuild.set("233")

    // The public Marketplace description is single-sourced from README.md,
    // between the "Plugin description" markers, so the listing and the repo
    // front page cannot drift apart.
    pluginDescription.set(providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
      val start = "<!-- Plugin description -->"
      val end = "<!-- Plugin description end -->"
      with(it.lines()) {
        val from = indexOf(start)
        val to = indexOf(end)
        if (from < 0 || to < 0 || from >= to) {
          throw GradleException("Plugin description markers missing or out of order in README.md:\n$start ... $end")
        }
        subList(from + 1, to).joinToString("\n").let(::markdownToHTML)
      }
    })

    // The change-notes are single-sourced from CHANGELOG.md: render the section
    // matching the release version, falling back to the Unreleased section.
    changeNotes.set(provider {
      with(changelog) {
        renderItem(
          (getOrNull(project.version.toString()) ?: getUnreleased())
            .withHeader(false)
            .withEmptySections(false),
          Changelog.OutputType.HTML,
        )
      }
    })
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
    // By default publishPlugin picks its archive on signPlugin's didWork flag, which
    // is false whenever signing is skipped, up to date, or restored from the build
    // cache. In those cases it silently uploads the unsigned archive and still
    // reports success. Point it at the signed archive so signing cannot be lost.
    archiveFile.set(signPlugin.flatMap { it.signedArchiveFile })
  }
}
