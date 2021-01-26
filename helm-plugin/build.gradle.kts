plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("org.jetbrains.dokka")
    id("maven-publish")
}


dependencies {

    implementation("org.yaml:snakeyaml:1.27")
    implementation("org.json:json:20200518")

    implementation("com.squareup.okhttp3:okhttp:4.9.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
    }
    implementation("com.squareup.okhttp3:okhttp-tls:4.9.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
    }

    implementation("org.unbroken-dome.gradle-plugin-utils:gradle-plugin-utils:0.1.0")

    testImplementation("com.jayway.jsonpath:json-path:2.4.0")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.11.2")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.11.2")

    testImplementation("com.squareup.okhttp3:mockwebserver:4.9.0")

    testImplementation("org.unbroken-dome.gradle-plugin-utils:gradle-plugin-test-utils:0.1.0")
}


gradlePlugin {

    plugins {
        create("helmCommandsPlugin") {
            id = "org.unbroken-dome.helm-commands"
            implementationClass = "org.unbrokendome.gradle.plugins.helm.command.HelmCommandsPlugin"
        }
        create("helmPlugin") {
            id = "org.unbroken-dome.helm"
            implementationClass = "org.unbrokendome.gradle.plugins.helm.HelmPlugin"
        }
        create("helmPublishPlugin") {
            id = "org.unbroken-dome.helm-publish"
            implementationClass = "org.unbrokendome.gradle.plugins.helm.publishing.HelmPublishPlugin"
        }
        create("helmReleasesPlugin") {
            id = "org.unbroken-dome.helm-releases"
            implementationClass = "org.unbrokendome.gradle.plugins.helm.release.HelmReleasesPlugin"
        }
    }
}


pluginBundle {
    (plugins) {
        "helmCommandsPlugin" {
            displayName = "Helm Commands plugin"
        }
        "helmPlugin" {
            displayName = "Helm plugin"
        }
        "helmPublishPlugin" {
            displayName = "Helm Publish Plugin"
        }
        "helmReleasesPlugin" {
            displayName = "Helm Releases Plugin"
        }
    }
}
