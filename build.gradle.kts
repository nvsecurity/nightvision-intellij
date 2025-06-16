import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "2.2.0-RC2"
  id("org.jetbrains.intellij.platform") version "2.6.0"
}

group = "net.nightvision"
version = "2.0-SNAPSHOT"


repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  intellijPlatform {
    intellijIdeaCommunity("2025.1.1.1")
    bundledPlugin("com.intellij.java")
    pluginVerifier()
  }
}

intellijPlatform {
  pluginVerification {
    ides.ides(listOf("IC-2025.1.1.1"))
  }
}

tasks.named<KotlinJvmCompile>("compileKotlin"){
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
    sinceBuild.set("241")
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
