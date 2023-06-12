package com.citi.gradle.plugins.helm.release.dsl

import org.gradle.api.Buildable
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskDependency
import com.citi.gradle.plugins.helm.rules.ChartDirArtifactRule


interface ChartReference : Buildable {

    val chartLocation: String


    override fun getBuildDependencies(): TaskDependency =
        TaskDependency { emptySet() }
}


/**
 * Simple implementation of [ChartReference] that directly contains a path or URL to the referenced chart,
 * and has no task dependencies.
 */
internal class SimpleChartReference(
    override val chartLocation: String
) : ChartReference


internal class HelmChartReference(
    private val project: Project,
    private val chartName: String
) : ChartReference {

    private val configuration: Configuration
        get() = project.configurations.getByName(ChartDirArtifactRule.getConfigurationName(chartName))


    private val artifact: PublishArtifact
        get() = configuration.artifacts.first()


    override val chartLocation: String
        get() = artifact.file.absolutePath


    override fun getBuildDependencies(): TaskDependency =
        TaskDependency { task ->
            artifact.buildDependencies.getDependencies(task)
        }
}


/**
 * Implementation of [ChartReference] for a [FileCollection] (including Gradle `Configuration`)
 *
 * The [FileCollection] is assumed to contain a single file. Build dependencies from the `FileCollection` will be
 * used by the chart reference.
 */
internal class FileCollectionChartReference(
    private val fileCollection: FileCollection
) : ChartReference {

    override val chartLocation: String
        get() = fileCollection.singleFile.absolutePath


    override fun getBuildDependencies(): TaskDependency =
        fileCollection.buildDependencies
}


/**
 * Implementation of [ChartReference] for a configuration that is lazily resolved.
 */
internal class ConfigurationChartReference(
    private val project: Project,
    private val configurationName: String
) : ChartReference {

    override val chartLocation: String
        get() = project.configurations.getByName(configurationName)
            .resolvedConfiguration
            .files
            .first()
            .absolutePath


    override fun getBuildDependencies(): TaskDependency =
        project.configurations.getByName(configurationName)
            .buildDependencies
}
