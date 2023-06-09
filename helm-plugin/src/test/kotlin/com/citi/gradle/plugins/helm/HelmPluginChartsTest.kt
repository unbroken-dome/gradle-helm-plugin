package com.citi.gradle.plugins.helm

import assertk.all
import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.each
import assertk.assertions.extracting
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.gradle.api.Task
import org.gradle.kotlin.dsl.lint
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import com.citi.gradle.plugins.helm.command.tasks.HelmLint
import com.citi.gradle.plugins.helm.command.tasks.HelmPackage
import com.citi.gradle.plugins.helm.command.tasks.HelmUpdateDependencies
import com.citi.gradle.plugins.helm.dsl.Filtering
import com.citi.gradle.plugins.helm.dsl.HelmChart
import com.citi.gradle.plugins.helm.dsl.Linting
import com.citi.gradle.plugins.helm.dsl.internal.charts
import com.citi.gradle.plugins.helm.dsl.internal.filtering
import com.citi.gradle.plugins.helm.dsl.internal.helm
import com.citi.gradle.plugins.helm.dsl.internal.lint
import com.citi.gradle.plugins.helm.tasks.HelmFilterSources
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.containsItem
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.containsTask
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.dirValue
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.doesNotContainItem
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.hasExtension
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.hasOnlyTaskDependency
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.hasTaskDependencies
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.hasValueEqualTo
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.isPresent
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.isPresentAndEmptyMap
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.taskDependencies
import org.unbrokendome.gradle.pluginutils.test.directory
import org.unbrokendome.gradle.pluginutils.test.spek.applyPlugin
import org.unbrokendome.gradle.pluginutils.test.spek.setupGradleProject


object HelmPluginChartsTest : Spek({

    val project by setupGradleProject {
        projectName = "awesome"
        applyPlugin<HelmPlugin>()
    }


    fun addChart(name: String = "myChart", chartName: String = "my-chart") {
        with(project.helm.charts) {
            create(name) { chart ->
                chart.chartName.set(chartName)
                chart.chartVersion.set("1.2.3")
                chart.sourceDir.set(project.file("src/$chartName"))
            }
        }
    }


    describe("when adding a chart") {

        beforeEachTest {
            addChart()
        }


        describe("chart lint extension") {

            it("should be added to each chart") {
                assertThat(project.helm.charts, "helmCharts")
                    .containsItem("myChart")
                    .hasExtension<Linting>("lint")
            }


            it("should inherit enabled flag from global lint options") {
                with(project.helm.lint) {
                    enabled.set(false)
                }

                assertThat(project.helm.charts, name = "helmCharts")
                    .containsItem("myChart")
                    .hasExtension<Linting>("lint")
                    .prop(Linting::enabled).hasValueEqualTo(false)
            }


            it("should inherit strict flag from global lint options") {
                with(project.helm.lint) {
                    strict.set(true)
                }

                assertThat(project.helm.charts, name = "helmCharts")
                    .containsItem("myChart")
                    .hasExtension<Linting>("lint")
                    .prop(Linting::strict).hasValueEqualTo(true)
            }


            it("should inherit values from global lint options") {
                with(project.helm.lint) {
                    values.put("foo", "bar")
                    fileValues.put("someText", "files/some-text.txt")
                    valueFiles.from("values/values.yaml")
                }

                assertThat(project.helm.charts, name = "helmCharts")
                    .containsItem("myChart")
                    .hasExtension<Linting>("lint").all {
                        prop(Linting::values).isPresent()
                            .containsOnly("foo" to "bar")
                        prop(Linting::fileValues).isPresent()
                            .containsOnly("someText" to "files/some-text.txt")
                        prop(Linting::valueFiles)
                            .containsOnly(project.file("values/values.yaml"))
                    }
            }
        }


        describe("chart filtering extension") {

            it("should be added to each chart") {
                assertThat(project.helm.charts, name = "helmCharts")
                    .containsItem("myChart")
                    .hasExtension<Filtering>("filtering")
            }


            it("should inherit enabled flag from global filtering options") {

                with(project.helm.filtering) {
                    enabled.set(false)
                }

                assertThat(project.helm.charts, name = "helmCharts")
                    .containsItem("myChart")
                    .hasExtension<Filtering>("filtering")
                    .prop(Filtering::enabled).hasValueEqualTo(false)
            }


            it("should inherit values from global filtering options") {

                with(project.helm.filtering) {
                    values.put("foo", "bar")
                    fileValues.put("someText", "files/some-text.txt")
                }

                assertThat(project.helm.charts, name = "helmCharts")
                    .containsItem("myChart")
                    .hasExtension<Filtering>("filtering").all {
                        prop(Filtering::values).isPresent()
                            .containsOnly("foo" to "bar")
                        prop(Filtering::fileValues).isPresent()
                            .containsOnly("someText" to "files/some-text.txt")
                    }
            }
        }


        describe("tasks for chart") {

            it("should create a HelmFilterSources task for each chart") {

                assertThat(project, name = "project")
                    .containsTask<HelmFilterSources>("helmFilterMyChartChartSources")
                    .all {
                        prop(HelmFilterSources::chartName).isPresent().isEqualTo("my-chart")
                        prop(HelmFilterSources::chartVersion).isPresent().isEqualTo("1.2.3")
                        prop(HelmFilterSources::sourceDir).dirValue()
                            .isEqualTo(project.projectDir.resolve("src/my-chart"))
                    }
            }


            it("should create a HelmUpdateDependencies task for each chart") {

                assertThat(project, name = "project")
                    .containsTask<HelmUpdateDependencies>("helmUpdateMyChartChartDependencies")
                    .all {
                        prop(HelmUpdateDependencies::chartDir).dirValue()
                            .isEqualTo(project.buildDir.resolve("helm/charts/my-chart"))
                    }
            }


            it("should create a HelmPackage task for each chart") {

                assertThat(project, name = "project")
                    .containsTask<HelmPackage>("helmPackageMyChartChart")
                    .all {
                        prop(HelmPackage::chartName).isPresent().isEqualTo("my-chart")
                        prop(HelmPackage::chartVersion).isPresent().isEqualTo("1.2.3")
                        prop(HelmPackage::sourceDir).dirValue()
                            .isEqualTo(project.buildDir.resolve("helm/charts/my-chart"))
                    }
            }
        }


        describe("linting tasks") {

            describe("without any linting configurations") {

                it("should have a default linting configuration for the chart") {

                    val chart = project.helm.charts.getByName("myChart")

                    assertThat(chart, name = "chart")
                        .prop("lint") { it.lint }
                        .prop("configurations") { it.configurations }.all {
                            containsItem("default")
                            hasSize(1)
                        }
                }


                it("should create a HelmLint task for the chart and default lint configuration") {
                    assertThat(project, name = "project")
                        .containsTask<HelmLint>("helmLintMyChartChartDefault")
                        .all {
                            prop(HelmLint::chartDir).dirValue()
                                .isEqualTo(project.buildDir.resolve("helm/charts/my-chart"))
                            prop(HelmLint::values).isPresentAndEmptyMap()
                            prop(HelmLint::fileValues).isPresentAndEmptyMap()
                            prop(HelmLint::valueFiles).isEmpty()
                        }
                }


                it("should create a HelmLint task for the chart") {

                    assertThat(project, name = "project")
                        .containsTask<Task>("helmLintMyChartChart")
                        .hasOnlyTaskDependency("helmLintMyChartChartDefault")
                }
            }


            describe("with custom linting configurations") {

                beforeEachTest {
                    val chart = project.helm.charts.getByName("myChart")
                    with(chart.lint) {
                        configurations.register("config1")
                        configurations.register("config2")
                    }
                }

                it("should not have a default linting configuration for the chart") {

                    val chart = project.helm.charts.getByName("myChart")

                    assertThat(chart, name = "chart")
                        .prop("lint") { it.lint }
                        .prop("configurations") { it.configurations }
                        .doesNotContainItem("default")
                }


                it("should create a HelmLint task for the chart and each lint configuration") {
                    assertThat(project, name = "project").all {
                        containsTask<HelmLint>("helmLintMyChartChartConfig1")
                            .prop(HelmLint::chartDir).dirValue()
                            .isEqualTo(project.buildDir.resolve("helm/charts/my-chart"))
                        containsTask<HelmLint>("helmLintMyChartChartConfig2")
                            .prop(HelmLint::chartDir).dirValue()
                            .isEqualTo(project.buildDir.resolve("helm/charts/my-chart"))
                    }
                }


                it("should create a HelmLint task for the chart") {

                    assertThat(project, name = "project")
                        .containsTask<Task>("helmLintMyChartChart")
                        .hasTaskDependencies(
                            "helmLintMyChartChartConfig1", "helmLintMyChartChartConfig2",
                            exactly = true
                        )
                }
            }
        }
    }


    describe("helmPackage task") {

        beforeEachTest {
            addChart(name = "foo", chartName = "foo")
            addChart(name = "bar", chartName = "bar")
        }

        it("should create a helmPackage task that packages all charts") {

            assertThat(project, name = "project")
                .containsTask<Task>("helmPackage")
                .taskDependencies.all {
                    each { it.isInstanceOf(HelmPackage::class) }
                    extracting { it.name }.containsOnly("helmPackageFooChart", "helmPackageBarChart")
                }
        }
    }


    describe("main chart") {

        beforeEachTest {
            project.version = "2.5.9"
        }


        it("should not create a \"main\" chart if chart sources don't exist") {

            assertThat(project.helm.charts, name = "helmCharts")
                .doesNotContainItem("main")
        }


        describe("when sources exist") {

            beforeEachTest {
                directory(project.projectDir) {
                    directory("src/main/helm") {
                        file("Chart.yaml", contents = "apiVersion: v2")
                    }
                }
            }


            it("should create a \"main\" chart if no other charts are declared") {

                assertThat(project.helm.charts, name = "helmCharts")
                    .containsItem("main")
                    .all {
                        prop(HelmChart::chartName).isPresent().isEqualTo("awesome")
                        prop(HelmChart::chartVersion).isPresent().isEqualTo("2.5.9")
                        prop(HelmChart::sourceDir).dirValue()
                            .isEqualTo(project.projectDir.resolve("src/main/helm"))
                    }
            }


            it("should not create a \"main\" chart if other charts are declared") {

                addChart()

                assertThat(project.helm.charts, name = "helmCharts")
                    .doesNotContainItem("main")
            }
        }
    }
})
