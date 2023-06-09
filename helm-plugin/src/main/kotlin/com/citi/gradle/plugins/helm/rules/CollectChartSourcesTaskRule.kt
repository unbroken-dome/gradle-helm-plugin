package com.citi.gradle.plugins.helm.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskContainer
import com.citi.gradle.plugins.helm.HELM_GROUP
import com.citi.gradle.plugins.helm.dsl.HelmChart
import com.citi.gradle.plugins.helm.dsl.HelmChartInternal
import org.unbrokendome.gradle.pluginutils.asFile
import org.unbrokendome.gradle.pluginutils.rules.RuleNamePattern


private val namePattern =
    RuleNamePattern.parse("helmCollect<Chart>ChartSources")


/**
 * The name of the [Sync] task to copy the extra files into this chart.
 */
val HelmChart.collectChartSourcesTaskName
    get() = namePattern.mapName(name)


/**
 * A rule that creates a [Sync] task to collect all the sources of a chart into the final chart directory:
 *
 * - the filtered chart sources (output from the `HelmFilterSources` task)
 * - the internal chart dependencies
 */
internal class CollectChartSourcesTaskRule(
    tasks: TaskContainer,
    charts: NamedDomainObjectCollection<HelmChart>
) : AbstractHelmChartTaskRule<Sync>(
    Sync::class.java, tasks, charts, namePattern
) {

    override fun Sync.configureFrom(chart: HelmChart) {

        group = HELM_GROUP
        description = "Collects all sources for the ${chart.name} chart."
        includeEmptyDirs = false

        into(chart.outputDir.asFile())
        from((chart as HelmChartInternal).filteredSourcesDir.asFile())

        from(chart.dependenciesDir) { spec ->
            spec.into("charts")
        }

        with(chart.extraFiles)

        dependsOn(
            chart.filterSourcesTaskName,
            chart.collectDependenciesTaskName
        )

        // Preserve any .lock files placed by helm dep build or helm dep up
        preserve {
            it.include("*.lock")
        }
    }
}
