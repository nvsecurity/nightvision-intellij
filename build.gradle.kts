import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "2.3.20"
  id("org.jetbrains.intellij.platform") version "2.6.0"
  // Applied for the markdownToHTML function used by patchPluginXml below.
  id("org.jetbrains.changelog") version "2.5.0"
}

group = "net.nightvision"
version = "2.2.0"


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
        if (!containsAll(listOf(start, end))) {
          throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
        }
        subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
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
  }
}
