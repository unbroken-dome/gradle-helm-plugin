import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm") version "1.3.10"
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "0.10.0"
    id("org.jetbrains.dokka") version "0.9.17"
    id("maven-publish")
}


repositories {
    jcenter()
}


dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    implementation("org.yaml:snakeyaml:1.18")
    implementation("org.apache.httpcomponents:httpclient:4.5.6")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.2.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.2.0")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.11")

    testImplementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("reflect"))
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=enable")
}


tasks.withType<Test> {
    // always execute tests
    outputs.upToDateWhen { false }

    useJUnitPlatform()

    // give tests a temporary directory below the build dir so
    // we don't pollute the system temp dir (Gradle tests don't clean up)
    systemProperty("java.io.tmpdir", layout.buildDirectory.dir("tmp").get())
}


gradlePlugin {

    isAutomatedPublishing = true

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
    website = "https://github.com/unbroken-dome/gradle-helm-plugin"
    vcsUrl = "https://github.com/unbroken-dome/gradle-helm-plugin"
    description = "A suite of Gradle plugins for building, publishing and managing Helm charts."
    tags = listOf("helm")

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
