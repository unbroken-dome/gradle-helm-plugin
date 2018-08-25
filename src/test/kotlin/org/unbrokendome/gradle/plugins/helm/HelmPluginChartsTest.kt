package org.unbrokendome.gradle.plugins.helm

import assertk.all
import assertk.assert
import assertk.assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmBuildDependencies
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmLint
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmPackage
import org.unbrokendome.gradle.plugins.helm.dsl.*
import org.unbrokendome.gradle.plugins.helm.tasks.HelmFilterSources
import org.unbrokendome.gradle.plugins.helm.testutil.*


@Suppress("NestedLambdaShadowedImplicitParameter")
class HelmPluginChartsTest : AbstractGradleProjectTest() {

    @BeforeEach
    fun applyPlugin() {
        project.plugins.apply(HelmPlugin::class.java)
    }


    @Test
    fun `Plugin should add a lint extension to each chart`() {
        addChart()

        val chart = project.helm.charts.getByName("myChart")
        assert(chart, name = "chart").hasExtension<Linting>("lint")
    }


    @Test
    fun `Chart's lint should inherit from global lint`() {
        addChart()
        with(project.helm.lint) {
            values.put("foo", "bar")
        }

        val chart = project.helm.charts.getByName("myChart")
        val lintValues = chart.lint.values.orNull
        assert(lintValues, name = "lint.values")
                .isNotNull {
                    it.contains("foo", "bar")
                }
    }


    @Test
    fun `Plugin should add a filtering extension to each chart`() {
        addChart()

        val chart = project.helm.charts.getByName("myChart")
        assert(chart, name = "chart").hasExtension<Filtering>("filtering")
    }


    @Test
    fun `Chart's filtering should inherit from global filtering`() {
        addChart()
        with(project.helm.filtering) {
            placeholderPrefix.set("_O_o_")
        }

        val chart = project.helm.charts.getByName("myChart")
        val filterPrefix = chart.filtering.placeholderPrefix.get()
        assert(filterPrefix, name = "chart.filtering.placeholderPrefix").isEqualTo("_O_o_")
    }


    @Test
    fun `Plugin should create a HelmFilterSources task for each chart`() {
        addChart()

        val filterSourcesTask = project.tasks.findByName("helmFilterMyChartChartSources")
        assert(filterSourcesTask)
                .isInstanceOf(HelmFilterSources::class) {
                    it.prop(HelmFilterSources::chartName).hasValueEqualTo("my-chart")
                    it.prop(HelmFilterSources::chartVersion).hasValueEqualTo("1.2.3")
                    it.prop(HelmFilterSources::sourceDir)
                            .hasDirValueEqualTo(project.projectDir.resolve("src/my-chart"))
                }
    }


    @Test
    fun `Plugin should create a HelmBuildDependencies task for each chart`() {
        addChart()

        val buildDependenciesTask = project.tasks.findByName("helmBuildMyChartChartDependencies")
        assert(buildDependenciesTask)
                .isInstanceOf(HelmBuildDependencies::class) {
                    it.prop(HelmBuildDependencies::chartDir)
                            .hasDirValueEqualTo(project.buildDir.resolve("helm/charts/my-chart"))
                }
    }


    @Test
    fun `Plugin should create a HelmLint task for each chart`() {
        addChart()

        val lintTask = project.tasks.findByName("helmLintMyChartChart")
        assert(lintTask)
                .isInstanceOf(HelmLint::class) {
                    it.prop(HelmLint::chartDir)
                            .hasDirValueEqualTo(project.buildDir.resolve("helm/charts/my-chart"))
                }
    }


    @Test
    fun `Plugin should create a HelmPackage task for each chart`() {
        addChart()

        val packageTask = project.tasks.findByName("helmPackageMyChartChart")
        assert(packageTask)
                .isInstanceOf(HelmPackage::class) {
                    it.prop(HelmPackage::sourceDir)
                            .hasDirValueEqualTo(project.buildDir.resolve("helm/charts/my-chart"))
                }
    }


    @Test
    fun `Plugin should create a helmPackage task that packages all charts`() {
        addChart(name = "foo", chartName = "foo")
        addChart(name = "bar", chartName = "bar")

        val packageAllTask = project.tasks.findByName("helmPackage")
        assert(packageAllTask)
                .isNotNull {
                    it.prop("dependencies") { it.taskDependencies.getDependencies(it) }
                            .all {
                                hasSize(2)
                                each {
                                    it.isInstanceOf(HelmPackage::class)
                                }
                            }
                }
    }


    @Test
    @GradleProjectName("awesome")
    fun `Should create a "main" chart automatically`() {

        project.version = "2.5.9"

        evaluateProject()

        val mainChart = project.helm.charts.findByName("main")
        assert(mainChart, name = "main chart")
                .isNotNull {
                    it.prop(HelmChart::chartName).hasValueEqualTo("awesome")
                    it.prop(HelmChart::chartVersion).hasValueEqualTo("2.5.9")
                    it.prop(HelmChart::sourceDir).hasDirValueEqualTo(project.projectDir.resolve("src/main/helm"))
                }
    }


    @Test
    fun `Should not create a "main" chart if other charts are configured`() {
        addChart()
        evaluateProject()

        assert(project.helm.charts, name = "charts")
                .prop("main") { it.findByName("main") }
                .isNull()
    }


    private fun addChart(name: String = "myChart", chartName: String = "my-chart") {
        with(project.helm.charts) {
            create(name) { chart ->
                chart.chartName.set(chartName)
                chart.chartVersion.set("1.2.3")
                chart.sourceDir.set(project.file("src/$chartName"))
            }
        }
    }
}
