plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("org.jetbrains.dokka")
    id("maven-publish")
}


dependencies {

    implementation(libs.snakeyaml)
    implementation(libs.json)

    implementation(libs.plugin.utils)

    testImplementation(libs.jsonpath)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.jackson.dataformat.yaml)

    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.plugin.testutils)
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
