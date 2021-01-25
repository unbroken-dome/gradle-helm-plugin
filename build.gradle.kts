import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "0.12.0"
    id("org.jetbrains.dokka") version "1.4.20"
    id("maven-publish")
    id("org.asciidoctor.jvm.convert") version "3.2.0"
}


repositories {
    jcenter()
}


dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    implementation("org.yaml:snakeyaml:1.27")
    implementation("org.json:json:20200518")

    implementation("com.squareup.okhttp3:okhttp:4.9.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
    }
    implementation("com.squareup.okhttp3:okhttp-tls:4.9.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
    }

    implementation("org.unbroken-dome.gradle-plugin-utils:gradle-plugin-utils:0.1.0")

    testImplementation("org.spekframework.spek2:spek-dsl-jvm:2.0.9")
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:2.0.9")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.23")
    testImplementation("io.mockk:mockk:1.10.0")
    testImplementation("com.jayway.jsonpath:json-path:2.4.0")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.11.2")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.11.2")

    testImplementation("com.squareup.okhttp3:mockwebserver:4.9.0")

    testImplementation("org.unbroken-dome.gradle-plugin-utils:gradle-plugin-test-utils:0.1.0")

    testImplementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("reflect"))
}


configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(embeddedKotlinVersion)
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

    testLogging.showStandardStreams = true

    // give tests a temporary directory below the build dir so
    // we don't pollute the system temp dir (Gradle tests don't clean up)
    systemProperty("java.io.tmpdir", layout.buildDirectory.dir("tmp").get())

    maxParallelForks = (project.property("test.maxParallelForks") as String).toInt()
    if (maxParallelForks > 1) {
        // Parallel tests seem to need a little more time to set up, so increase the test timeout to
        // make sure that the first test in a forked process doesn't fail because of this
        systemProperty("SPEK_TIMEOUT", 30000)
    }
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


tasks.named("dokkaHtml", DokkaTask::class) {
    dokkaSourceSets.named("main") {
        externalDocumentationLink {
            url.set(uri("https://docs.oracle.com/javase/8/docs/api/").toURL())
        }
        externalDocumentationLink {
            url.set(uri("https://docs.gradle.org/current/javadoc/").toURL())
        }
        externalDocumentationLink {
            url.set(uri("https://docs.groovy-lang.org/latest/html/groovy-jdk/").toURL())
        }
        reportUndocumented.set(false)
        sourceLink {
            localDirectory.set(project.file("src/main/kotlin"))
            remoteUrl.set(
                uri("https://github.com/unbroken-dome/gradle-helm-plugin/blob/v${project.version}/src/main/kotlin").toURL()
            )
            remoteLineSuffix.set("#L")
        }
    }
}


val asciidoctorExt: Configuration by configurations.creating

dependencies {
    asciidoctorExt("com.bmuschko:asciidoctorj-tabbed-code-extension:0.3")
}


tasks.named("asciidoctor", org.asciidoctor.gradle.jvm.AsciidoctorTask::class) {

    sourceDir("docs")
    baseDirFollowsSourceDir()
    sources {
        include("index.adoc")
    }

    configurations(asciidoctorExt.name)

    options(
        mapOf(
            "doctype" to "book"
        )
    )
    attributes(
        mapOf(
            "project-version" to project.version,
            "source-highlighter" to "prettify"
        )
    )
}
