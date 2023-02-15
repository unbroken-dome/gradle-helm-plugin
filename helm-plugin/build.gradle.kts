plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("org.jetbrains.dokka")
    id("maven-publish")
}


dependencies {

    implementation("org.yaml:snakeyaml:1.33")
    implementation("org.json:json:20200518")

    implementation("org.unbroken-dome.gradle-plugin-utils:gradle-plugin-utils:0.5.0")

    testImplementation("com.jayway.jsonpath:json-path:2.4.0")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.11.2")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.11.2")

    testImplementation("com.squareup.okhttp3:mockwebserver:4.9.0")

    testImplementation("org.unbroken-dome.gradle-plugin-utils:gradle-plugin-test-utils:0.5.0")
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
    }
}
