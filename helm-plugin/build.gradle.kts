plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("org.jetbrains.dokka")
    id("maven-publish")
}


dependencies {

    implementation("org.yaml:snakeyaml:2.0")
    implementation("org.json:json:20230227")

    implementation("org.unbroken-dome.gradle-plugin-utils:gradle-plugin-utils:0.5.0")

    testImplementation("com.jayway.jsonpath:json-path:2.8.0")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")

    testImplementation("com.squareup.okhttp3:mockwebserver:4.11.0")

    testImplementation("org.unbroken-dome.gradle-plugin-utils:gradle-plugin-test-utils:0.5.0")
}


gradlePlugin {

    plugins {
        create("helmCommandsPlugin") {
            id = "com.citi.helm-commands"
            implementationClass = "com.citi.gradle.plugins.helm.command.HelmCommandsPlugin"
        }
        create("helmPlugin") {
            id = "com.citi.helm"
            implementationClass = "com.citi.gradle.plugins.helm.HelmPlugin"
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
