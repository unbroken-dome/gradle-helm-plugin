package org.unbrokendome.gradle.plugins.helm

import assertk.assert
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import org.junit.jupiter.api.Test
import org.unbrokendome.gradle.plugins.helm.dsl.*
import org.unbrokendome.gradle.plugins.helm.testutil.hasExtension


class HelmPluginChartsTest : AbstractGradleProjectTest() {

    @Test
    fun `Plugin should add a lint extension to each chart`() {
        applyPlugin()
        addChart()

        val chart = project.helm.charts.getByName("myChart")
        assert(chart, name = "chart").hasExtension<Linting>("lint")
    }


    @Test
    fun `Chart's lint should inherit from global lint`() {
        applyPlugin()
        addChart()
        with (project.helm.lint) {
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
        applyPlugin()
        addChart()

        val chart = project.helm.charts.getByName("myChart")
        assert(chart, name = "chart").hasExtension<Filtering>("filtering")
    }


    @Test
    fun `Chart's filtering should inherit from global filtering`() {
        applyPlugin()
        addChart()
        with (project.helm.filtering) {
            placeholderPrefix.set("_O_o_")
        }

        val chart = project.helm.charts.getByName("myChart")
        val filterPrefix = chart.filtering.placeholderPrefix.get()
        assert(filterPrefix, name = "chart.filtering.placeholderPrefix").isEqualTo("_O_o_")
    }



    private fun addChart() {
        with (project.helm.charts) {
            create("myChart") { chart ->
                chart.chartName.set("my-chart")
                chart.chartVersion.set("1.2.3")
                chart.sourceDir.set(project.file("src/my-chart"))
            }
        }
    }



    private fun applyPlugin() {
        project.plugins.apply(HelmPlugin::class.java)
    }
}
