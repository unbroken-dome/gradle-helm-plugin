plugins {
    kotlin("jvm") apply false
    alias(libs.plugins.gradlePublish) apply false
    id("org.jetbrains.dokka") version embeddedKotlinVersion
    alias(libs.plugins.asciidoctor)
    alias(libs.plugins.benManesVersions)
}


allprojects {
    repositories {
        mavenCentral()
    }
}


subprojects {
    plugins.withType<JavaGradlePluginPlugin> {
        dependencies {
            "compileOnly"(kotlin("stdlib"))
        }

        with(the<GradlePluginDevelopmentExtension>()) {
            isAutomatedPublishing = true
        }

        with(the<JavaPluginExtension>()) {
            withSourcesJar()
            withJavadocJar()
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
            "testImplementation"(kotlin("stdlib"))
            "testImplementation"(kotlin("reflect"))

            "testImplementation"(libs.assertk)
            "testImplementation"(libs.mockk)
            "testImplementation"(libs.spekDsl)
            "testRuntimeOnly"(libs.spekRunner)
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions.jvmTarget = "1.8"
        }

        tasks.withType<JavaCompile> {
            sourceCompatibility = "1.8"
            targetCompatibility = "1.8"
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

    plugins.withId("org.jetbrains.kotlinx.binary-compatibility-validator") {
        tasks.build.configure {
            dependsOn(tasks.named("apiCheck"))
        }
    }

    plugins.withId("org.jetbrains.dokka") {

        dependencies {
            "dokkaJavadocPlugin"("org.jetbrains.dokka:kotlin-as-java-plugin:$embeddedKotlinVersion")
        }

        // have an option to disable Dokka task for local builds
        if (project.findProperty("com.citi.gradle.helm.plugin.dokka.disabled") == "true") {
            logger.info("Dokka tasks are disabled")
        } else {
            tasks.withType<Jar>().matching { it.name == "javadocJar" || it.name == "publishPluginJavaDocsJar" }
                .all {
                    from(tasks.named("dokkaJavadoc"))
                }
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

        with(the<GradlePluginDevelopmentExtension>()) {
            website.set("https://citi.github.io/projects/gradle-helm-plugin/")
            vcsUrl.set(githubUrl)
            description = "A suite of Gradle plugins for building, publishing and managing Helm charts."
            plugins.forEach {plugin ->
                plugin.tags.add("helm")
            }
        }
    }
}


val asciidoctorExt: Configuration by configurations.creating

dependencies {
    asciidoctorExt(libs.tabbedCodeExtension)
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
