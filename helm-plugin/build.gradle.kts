plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("org.jetbrains.dokka")
    id("maven-publish")
}


dependencies {

    implementation("org.yaml:snakeyaml:1.33")
    implementation("org.json:json:20220320")

    implementation("org.unbroken-dome.gradle-plugin-utils:gradle-plugin-utils:0.5.0")

    testImplementation("com.jayway.jsonpath:json-path:2.4.0")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.12.7.1")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.11.2")

    testImplementation("com.squareup.okhttp3:mockwebserver:4.9.0")

    testImplementation("org.unbroken-dome.gradle-plugin-utils:gradle-plugin-test-utils:0.5.0")
}


gradlePlugin {

    plugins {
        create("helmCommandsPlugin") {
            id = "org.unbroken-dome.helm-commands"
            implementationClass = "org.unbrokendome.gradle.plugins.helm.command.HelmCommandsPlugin"
            displayName = "Helm Commands plugin"
            description = "A plugin to execute Helm commands from Gradle. This serves as the basis for other " +
                    "Helm-related plugins. Usually you want to use the org.unbroken-dome.helm plugin instead."
        }
        create("helmPlugin") {
            id = "org.unbroken-dome.helm"
            implementationClass = "org.unbrokendome.gradle.plugins.helm.HelmPlugin"
            displayName = "Helm plugin"
            description = "A Gradle plugin that allows building Helm charts from Gradle build scripts."
        }
    }
}
