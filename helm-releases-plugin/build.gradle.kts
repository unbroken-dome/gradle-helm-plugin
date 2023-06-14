plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("org.jetbrains.dokka")
    id("maven-publish")
    alias(libs.plugins.binaryCompatibilityValidator)
}


dependencies {

    implementation(project(":helm-plugin"))

    implementation(libs.unbrokenDomePluginUtils)
    testImplementation(libs.unbrokenDomeTestUtils)
}


gradlePlugin {
    plugins {
        create("helmReleasesPlugin") {
            id = "com.citi.helm-releases"
            displayName = "Helm Releases Plugin"
            implementationClass = "com.citi.gradle.plugins.helm.release.HelmReleasesPlugin"
        }
    }
}
