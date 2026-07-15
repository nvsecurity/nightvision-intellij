import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "2.3.20"
  id("org.jetbrains.intellij.platform") version "2.6.0"
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
