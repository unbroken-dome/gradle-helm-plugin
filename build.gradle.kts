plugins {
    kotlin("jvm") apply false
    id("com.gradle.plugin-publish") version "0.21.0" apply false
    id("org.jetbrains.dokka") version "1.4.32"
    id("org.asciidoctor.jvm.convert") version "3.2.0"
}


allprojects {
    repositories {
        mavenCentral()
    }
}


subprojects {

    plugins.withType<JavaGradlePluginPlugin> {
        dependencies {
            "compileOnly"(kotlin("stdlib-jdk8"))
        }

        with(the<GradlePluginDevelopmentExtension>()) {
            isAutomatedPublishing = true
        }

        with(the<JavaPluginExtension>()) {
            withSourcesJar()
            withJavadocJar()

            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
    }


    plugins.withId("org.jetbrains.kotlin.jvm") {

        configurations.all {
            resolutionStrategy.eachDependency {
                if (requested.group == "org.jetbrains.kotlin") {
                    useVersion(embeddedKotlinVersion)
                }
            }
        }

        dependencies {
            "testImplementation"(kotlin("stdlib-jdk8"))
            "testImplementation"(kotlin("reflect"))

            "testImplementation"("com.willowtreeapps.assertk:assertk-jvm:0.23")
            "testImplementation"("io.mockk:mockk:1.10.0")
            "testImplementation"("org.spekframework.spek2:spek-dsl-jvm:2.0.9")
            "testRuntimeOnly"("org.spekframework.spek2:spek-runner-junit5:2.0.9")
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions.jvmTarget = "1.8"
            kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=enable")
        }

        tasks.withType<Test> {
            // always execute tests
            outputs.upToDateWhen { false }

            useJUnitPlatform()

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
    }


    plugins.withId("org.jetbrains.dokka") {

        dependencies {
            "dokkaJavadocPlugin"("org.jetbrains.dokka:kotlin-as-java-plugin:1.4.32")
        }

        tasks.withType<Jar>().matching { it.name == "javadocJar" || it.name == "publishPluginJavaDocsJar" }
            .all {
                from(tasks.named("dokkaJavadoc"))
            }

        tasks.withType<org.jetbrains.dokka.gradle.DokkaTask> {
            dokkaSourceSets.all {
                externalDocumentationLink {
                    url.set(uri("https://docs.oracle.com/javase/8/docs/api/").toURL())
                }
                reportUndocumented.set(false)

                val sourceSetName = this.name
                val githubUrl = project.extra["github.url"] as String

                sourceLink {
                    localDirectory.set(project.file("src/$sourceSetName/kotlin"))
                    remoteUrl.set(
                        uri("$githubUrl/blob/v${project.version}/${project.projectDir.relativeTo(rootDir)}/src/$sourceSetName/kotlin").toURL()
                    )
                    remoteLineSuffix.set("#L")
                }
            }
        }

        plugins.withType<JavaGradlePluginPlugin> {
            tasks.withType<org.jetbrains.dokka.gradle.DokkaTask> {
                dokkaSourceSets.all {
                    externalDocumentationLink {
                        url.set(uri("https://docs.gradle.org/current/javadoc/").toURL())
                    }
                    externalDocumentationLink {
                        url.set(uri("https://docs.groovy-lang.org/latest/html/groovy-jdk/").toURL())
                    }
                }
            }
        }
    }


    plugins.withId("com.gradle.plugin-publish") {

        val githubUrl = project.extra["github.url"] as String

        with(the<com.gradle.publish.PluginBundleExtension>()) {

            website = githubUrl
            vcsUrl = githubUrl
            description = "A suite of Gradle plugins for building, publishing and managing Helm charts."
            tags = listOf("helm")
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
