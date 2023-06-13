plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("org.jetbrains.dokka")
    id("maven-publish")
}


dependencies {

    implementation(project(":helm-plugin"))

    implementation("com.squareup.okhttp3:okhttp:4.11.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
    }
    implementation("com.squareup.okhttp3:okhttp-tls:4.11.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
    }

    implementation("org.unbroken-dome.gradle-plugin-utils:gradle-plugin-utils:0.5.0")

    testImplementation("org.unbroken-dome.gradle-plugin-utils:gradle-plugin-test-utils:0.5.0")
}


gradlePlugin {

    plugins {
        create("helmPublishPlugin") {
            id = "com.citi.helm-publish"
            implementationClass = "com.citi.gradle.plugins.helm.publishing.HelmPublishPlugin"
        }
    }
}


pluginBundle {
    (plugins) {
        "helmPublishPlugin" {
            displayName = "Helm Publish Plugin"
        }
    }
}
