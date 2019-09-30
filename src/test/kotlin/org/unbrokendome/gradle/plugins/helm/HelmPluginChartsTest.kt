package org.unbrokendome.gradle.plugins.helm

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Task
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmBuildOrUpdateDependencies
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmLint
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmPackage
import org.unbrokendome.gradle.plugins.helm.dsl.*
import org.unbrokendome.gradle.plugins.helm.tasks.HelmFilterSources
import org.unbrokendome.gradle.plugins.helm.testutil.assertions.*


class HelmPluginChartsTest : AbstractGradleProjectTest() {

    @BeforeEach
    fun applyPlugin() {
        project.plugins.apply(HelmPlugin::class.java)
    }

    private val helmCharts: NamedDomainObjectContainer<HelmChart>
        get() = project.helm.charts


    @Test
    fun `Plugin should add a lint extension to each chart`() {
        addChart()

        assertThat(this::helmCharts)
            .containsItem("myChart")
            .hasExtension<Linting>("lint")
    }


    @Test
    fun `Chart's lint should inherit from global lint`() {
        addChart()
        with(project.helm.lint) {
            values.put("foo", "bar")
        }

        assertThat(this::helmCharts)
            .containsItem("myChart")
            .hasExtension<Linting>("lint")
            .prop(Linting::values).isPresent()
            .contains("foo", "bar")
    }


    @Test
    fun `Plugin should add a filtering extension to each chart`() {
        addChart()

        assertThat(this::helmCharts)
            .containsItem("myChart")
            .hasExtension<Filtering>("filtering")
    }


    @Test
    fun `Chart's filtering should inherit from global filtering`() {
        addChart()
        with(project.helm.filtering) {
            placeholderPrefix.set("_O_o_")
        }

        assertThat(this::helmCharts)
            .containsItem("myChart")
            .hasExtension<Filtering>("filtering")
            .prop(Filtering::placeholderPrefix).isPresent()
            .isEqualTo("_O_o_")
    }


    @Test
    fun `Plugin should create a HelmFilterSources task for each chart`() {
        addChart()

        assertThat(this::project)
            .containsTask<HelmFilterSources>("helmFilterMyChartChartSources")
            .all {
                prop(HelmFilterSources::chartName).isPresent().isEqualTo("my-chart")
                prop(HelmFilterSources::chartVersion).isPresent().isEqualTo("1.2.3")
                prop(HelmFilterSources::sourceDir).dirValue()
                    .isEqualTo(project.projectDir.resolve("src/my-chart"))
            }
    }


    @Test
    fun `Plugin should create a HelmBuildOrUpdateDependencies task for each chart`() {
        addChart()

        assertThat(this::project)
            .containsTask<HelmBuildOrUpdateDependencies>("helmUpdateMyChartChartDependencies")
            .all {
                prop(HelmBuildOrUpdateDependencies::chartDir).dirValue()
                    .isEqualTo(project.buildDir.resolve("helm/charts/my-chart"))
            }
    }


    @Test
    fun `Plugin should create a HelmLint task for each chart`() {
        addChart()

        assertThat(this::project)
            .containsTask<HelmLint>("helmLintMyChartChart")
            .all {
                prop(HelmLint::chartDir).dirValue()
                    .isEqualTo(project.buildDir.resolve("helm/charts/my-chart"))
            }
    }


    @Test
    fun `Plugin should create a HelmPackage task for each chart`() {
        addChart()

        assertThat(this::project)
            .containsTask<HelmPackage>("helmPackageMyChartChart")
            .all {
                prop(HelmPackage::chartName).isPresent().isEqualTo("my-chart")
                prop(HelmPackage::chartVersion).isPresent().isEqualTo("1.2.3")
                prop(HelmPackage::sourceDir).dirValue()
                    .isEqualTo(project.buildDir.resolve("helm/charts/my-chart"))
            }
    }


    @Test
    fun `Plugin should create a helmPackage task that packages all charts`() {
        addChart(name = "foo", chartName = "foo")
        addChart(name = "bar", chartName = "bar")

        assertThat(this::project)
            .containsTask<Task>("helmPackage")
            .taskDependencies.all {
                each { it.isInstanceOf(HelmPackage::class) }
                extracting { it.name }.containsOnly("helmPackageFooChart", "helmPackageBarChart")
            }
    }


    @Test
    @GradleProjectName("awesome")
    fun `Should create a "main" chart automatically`() {

        project.version = "2.5.9"

        evaluateProject()

        assertThat(this::helmCharts)
            .containsItem("main")
            .all {
                prop(HelmChart::chartName).isPresent().isEqualTo("awesome")
                prop(HelmChart::chartVersion).isPresent().isEqualTo("2.5.9")
                prop(HelmChart::sourceDir).dirValue()
                    .isEqualTo(project.projectDir.resolve("src/main/helm"))
            }
    }


    @Test
    fun `Should not create a "main" chart if other charts are configured`() {
        addChart()
        evaluateProject()

        assertThat(this::helmCharts)
            .doesNotContainItem("main")
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
