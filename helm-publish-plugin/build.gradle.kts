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

    implementation(libs.okHttp) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
    }
    implementation(libs.okHttpTls) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
    }

    implementation(libs.unbrokenDomePluginUtils)

    testImplementation(libs.unbrokenDomeTestUtils)
}


gradlePlugin {
    plugins {
        create("helmPublishPlugin") {
            id = "com.citi.helm-publish"
            implementationClass = "com.citi.gradle.plugins.helm.publishing.HelmPublishPlugin"
        }
    }
}

apiValidation {
    ignoredPackages.add("com.citi.gradle.plugins.helm.publishing.dsl.internal")
}