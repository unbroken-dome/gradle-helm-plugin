plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("org.jetbrains.dokka")
    id("maven-publish")
    alias(libs.plugins.detekt)
    alias(libs.plugins.binaryCompatibilityValidator)
}

val functionalTest by sourceSets.creating

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

    "functionalTestImplementation"(libs.kotestAssertions)
    "functionalTestImplementation"(libs.junitApi)
    "functionalTestRuntimeOnly"(libs.junitEngine)
}


gradlePlugin {
    testSourceSets(functionalTest)
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

val functionalTestTask = tasks.register<Test>("functionalTest") {
    description = "Runs the integration tests."
    group = "verification"
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    mustRunAfter(tasks.test)
}

tasks.build {
    dependsOn(functionalTestTask)
}