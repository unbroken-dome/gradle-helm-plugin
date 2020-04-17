import org.asciidoctor.gradle.AsciidoctorTask
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL


plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "0.11.0"
    id("org.jetbrains.dokka") version "0.9.17"
    id("maven-publish")
    id("org.asciidoctor.convert") version "1.5.9.2"
}


repositories {
    jcenter()
}


dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    implementation("org.yaml:snakeyaml:1.25")
    implementation("org.json:json:20190722")
    implementation("org.apache.httpcomponents:httpclient:4.5.6")

    testImplementation("org.spekframework.spek2:spek-dsl-jvm:2.0.9")
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:2.0.9")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.22")
    testImplementation("io.mockk:mockk:1.9.3")
    testImplementation("com.jayway.jsonpath:json-path:2.4.0")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.9.10")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.10")

    testImplementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("reflect"))
}

val kotlinVersion: String by extra
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(kotlinVersion)
        }
    }
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=enable")
}


tasks.withType<Test> {
    // always execute tests
    outputs.upToDateWhen { false }

    useJUnitPlatform {
        includeEngines("spek2")
    }

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


tasks.named("dokka", DokkaTask::class) {
    outputFormat = "html"
    externalDocumentationLink(delegateClosureOf<DokkaConfiguration.ExternalDocumentationLink.Builder> {
        url = URL("https://docs.gradle.org/current/javadoc/")
    })
    reportUndocumented = false
}


asciidoctorj {
    version = "1.6.0"
}

dependencies {
    "asciidoctor"("com.bmuschko:asciidoctorj-tabbed-code-extension:0.2")
}


tasks.named("asciidoctor", AsciidoctorTask::class) {
    sourceDir("docs")
    sources(delegateClosureOf<PatternSet> { include("index.adoc") })

    options(mapOf(
            "doctype" to "book"
    ))
    attributes(mapOf(
            "project-version" to project.version,
            "source-highlighter" to "prettify"
    ))
}
