import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("jvm") apply false
    id("com.gradle.plugin-publish") version "1.2.1" apply false
    id("org.jetbrains.dokka")
    id("org.asciidoctor.jvm.convert") version "3.2.0"
}


subprojects {

    plugins.withType<JavaPlugin> {

        configure<JavaPluginExtension> {
            withSourcesJar()
            withJavadocJar()
        }
    }


    plugins.withId("org.jetbrains.kotlin.jvm") {

        configure<KotlinJvmProjectExtension> {

            jvmToolchain(11)

            compilerOptions {
                freeCompilerArgs.add("-Xjvm-default=all")
            }
        }


        configurations.all {
            resolutionStrategy.eachDependency {
                if (requested.group == "org.jetbrains.kotlin") {
                    useVersion(embeddedKotlinVersion)
                }
            }
        }

        dependencies {
            "compileOnly"(kotlin("stdlib"))

            "testImplementation"(kotlin("stdlib"))
            "testImplementation"(kotlin("reflect"))

            "testImplementation"(libs.assertk)
            "testImplementation"(libs.mockk)
            "testImplementation"(libs.spek.dsl)
            "testRuntimeOnly"(libs.spek.runner)
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

        val dokkaVersion: String by extra
        dependencies {
            "dokkaJavadocPlugin"("org.jetbrains.dokka:kotlin-as-java-plugin:$dokkaVersion")
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


    plugins.withType<JavaGradlePluginPlugin>() {

        val githubUrl = project.extra["github.url"] as String

        @Suppress("UnstableApiUsage")
        with(the<GradlePluginDevelopmentExtension>()) {
            website.set(githubUrl)
            vcsUrl.set(githubUrl)
            isAutomatedPublishing = true

            plugins.all {
                tags.add("helm")
            }
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
