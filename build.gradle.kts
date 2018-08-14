import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm") version "1.2.41"
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "0.10.0"
    id("org.jetbrains.dokka") version "0.9.17"
}


repositories {
    jcenter()
}


dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs = listOf("-Xenable-jvm-default", "-Xjvm-default=enable")
}
