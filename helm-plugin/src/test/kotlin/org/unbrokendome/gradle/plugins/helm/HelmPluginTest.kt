package com.citi.gradle.plugins.helm

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Task
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import com.citi.gradle.plugins.helm.command.tasks.HelmAddRepository
import com.citi.gradle.plugins.helm.command.tasks.HelmTemplate
import com.citi.gradle.plugins.helm.dsl.Filtering
import com.citi.gradle.plugins.helm.dsl.HelmChart
import com.citi.gradle.plugins.helm.dsl.HelmExtension
import com.citi.gradle.plugins.helm.dsl.HelmRepository
import com.citi.gradle.plugins.helm.dsl.internal.charts
import com.citi.gradle.plugins.helm.dsl.internal.helm
import com.citi.gradle.plugins.helm.dsl.internal.repositories
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.*
import org.unbrokendome.gradle.pluginutils.test.evaluate
import org.unbrokendome.gradle.pluginutils.test.spek.applyPlugin
import org.unbrokendome.gradle.pluginutils.test.spek.setupGradleProject
import java.net.URI


object HelmPluginTest : Spek({

    val project by setupGradleProject { applyPlugin<HelmPlugin>() }


    describe("applying the helm plugin") {

        it("project can be evaluated successfully") {
            assertThat {
                project.evaluate()
            }.isSuccess()
        }


        it("should create a helm DSL extension") {
            assertThat(project)
                .hasExtension<HelmExtension>("helm")
        }


        it("should create a helm filtering DSL extension") {
            assertThat(project)
                .hasExtension<HelmExtension>("helm")
                .hasExtension<Filtering>("filtering")
        }


        it("should create a helm charts DSL extension") {
            assertThat(project)
                .hasExtension<HelmExtension>("helm")
                .hasExtension<NamedDomainObjectContainer<HelmChart>>("charts")
        }
    }


    describe("renderings") {
        it("should create a default rendering") {
            val chart = project.helm.charts.create("my-chart")
            assertThat(chart.renderings)
                .containsItem("default")
        }

        it("should create a HelmTemplate task for each rendering") {
            val chart = project.helm.charts.create("foo")
            chart.renderings.create("red")

            assertThat(project)
                .containsTask<HelmTemplate>("helmRenderFooChartRedRendering")
        }

        it("should create a task that renders all renderings for a chart") {
            val chart = project.helm.charts.create("foo")
            chart.renderings.create("red")
            chart.renderings.create("yellow")

            assertThat(project)
                .containsTask<Task>("helmRenderFooChart")
                .hasTaskDependencies(
                    "helmRenderFooChartDefaultRendering",
                    "helmRenderFooChartRedRendering",
                    "helmRenderFooChartYellowRendering",
                    exactly = true
                )
        }

        it("should create a task that renders all renderings for all charts") {
            with(project.helm.charts) {
                create("foo")
                create("bar")
            }

            assertThat(project)
                .containsTask<Task>("helmRender")
                .hasTaskDependencies(
                    "helmRenderFooChart",
                    "helmRenderBarChart",
                    exactly = true
                )
        }
    }


    describe("repositories") {

        it("should create a helm repositories DSL extension") {
            assertThat(project)
                .hasExtension<HelmExtension>("helm")
                .hasExtension<NamedDomainObjectContainer<HelmRepository>>("repositories")
        }


        it("should create a HelmAddRepository task for each repository") {
            with(project.helm.repositories) {
                create("myRepo") { repo ->
                    repo.url.set(project.uri("http://repository.example.com"))
                }
            }

            assertThat(project)
                .containsTask<HelmAddRepository>("helmAddMyRepoRepository")
                .prop(HelmAddRepository::url)
                .isPresent().isEqualTo(URI("http://repository.example.com"))
        }


        it("should create a helmAddRepositories task that registers all repos") {

            with(project.helm.repositories) {
                create("myRepo1") { repo ->
                    repo.url.set(project.uri("http://repository1.example.com"))
                }
                create("myRepo2") { repo ->
                    repo.url.set(project.uri("http://repository2.example.com"))
                }
            }

            assertThat(project)
                .containsTask<Task>("helmAddRepositories")
                .taskDependencies.all {
                    each { it.isInstanceOf(HelmAddRepository::class) }
                    extracting { it.name }.containsOnly("helmAddMyRepo1Repository", "helmAddMyRepo2Repository")
                }
        }
    }
})
