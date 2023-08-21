plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("org.jetbrains.dokka")
    id("maven-publish")
}


dependencies {

    implementation(project(":helm-plugin"))

    implementation(libs.plugin.utils)
    testImplementation(libs.plugin.testutils)
}


gradlePlugin {

    plugins {
        create("helmReleasesPlugin") {
            id = "org.unbroken-dome.helm-releases"
            implementationClass = "org.unbrokendome.gradle.plugins.helm.release.HelmReleasesPlugin"
            displayName = "Helm Releases Plugin"
            description = "A Gradle plugin that manages Helm releases on a Kubernetes cluster."
        }
    }
}
