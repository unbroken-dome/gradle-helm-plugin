import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm") version "1.2.41"
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "0.10.0"
    id("org.jetbrains.dokka") version "0.9.17"
}


repositories {
    jcenter()
}


dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    implementation("org.yaml:snakeyaml:1.18")

    testImplementation(kotlin("stdlib-jdk8"))
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.11")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.2.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.2.0")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.11")
    testImplementation("org.jetbrains.spek:spek-api:1.1.5")
    testRuntimeOnly("org.jetbrains.spek:spek-junit-platform-engine:1.1.5")

    testImplementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("reflect"))
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs = listOf("-Xenable-jvm-default", "-Xjvm-default=enable")
}


tasks.withType<Test> {
    // always execute tests
    outputs.upToDateWhen { false }

    useJUnitPlatform {
        includeEngines("spek")
    }

    // give tests a temporary directory below the build dir so
    // we don't pollute the system temp dir (Gradle tests don't clean up)
    systemProperty("java.io.tmpdir", layout.buildDirectory.dir("tmp").get())
}


gradlePlugin {

    isAutomatedPublishing = true

    (plugins) {
        "helmCommandsPlugin" {
            id = "org.unbroken-dome.helm-commands"
            implementationClass = "org.unbrokendome.gradle.plugins.helm.command.HelmCommandsPlugin"
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
    }
}
