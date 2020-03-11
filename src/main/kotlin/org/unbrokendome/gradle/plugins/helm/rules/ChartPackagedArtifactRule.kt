package org.unbrokendome.gradle.plugins.helm.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.tasks.TaskContainer
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmPackage
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart


private val namePattern =
    RuleNamePattern.parse("helm<Chart>Packaged")


/**
 * The name of the [Configuration] that contains the directory artifact for this chart.
 */
val HelmChart.packagedArtifactConfigurationName: String
    get() = namePattern.mapName(name)


/**
 * A rule that registers an artifact configuration and an artifact for a chart package.
 *
 * The artifact will contain a single file, which is the tar.gz package file of the chart.
 */
internal class ChartPackagedArtifactRule(
    configurations: ConfigurationContainer,
    private val tasks: TaskContainer,
    charts: NamedDomainObjectCollection<HelmChart>
) : AbstractPatternRule<HelmChart, Configuration>(
    configurations, charts, namePattern
) {
    constructor(project: Project, charts: NamedDomainObjectCollection<HelmChart>)
            : this(project.configurations, project.tasks, charts)


    override fun Configuration.configureFrom(source: HelmChart) {

        isCanBeResolved = false
        isCanBeConsumed = true

        val packageTask =
            tasks.named(source.packageTaskName, HelmPackage::class.java)

        outgoing { publications ->
            publications.artifact(packageTask.flatMap { it.chartOutputPath }) { artifact ->
                artifact.builtBy(packageTask)
                artifact.name = source.chartName.get()
                artifact.extension = "tgz"
            }
        }
    }
}
