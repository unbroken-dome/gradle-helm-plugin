plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("org.jetbrains.dokka")
    id("maven-publish")
}


dependencies {

    implementation(project(":helm-plugin"))

    implementation("org.unbroken-dome.gradle-plugin-utils:gradle-plugin-utils:0.5.0")
    testImplementation("org.unbroken-dome.gradle-plugin-utils:gradle-plugin-test-utils:0.5.0")
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
