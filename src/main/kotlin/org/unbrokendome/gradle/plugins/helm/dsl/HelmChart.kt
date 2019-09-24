package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.Buildable
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskDependency
import org.unbrokendome.gradle.plugins.helm.rules.packageTaskName
import org.unbrokendome.gradle.plugins.helm.util.property
import org.unbrokendome.gradle.plugins.helm.util.versionProvider
import javax.inject.Inject


/**
 * Represents a Helm chart built by this project.
 */
interface HelmChart : Named, Buildable {

    /**
     * The chart name.
     */
    val chartName: Property<String>


    /**
     * The chart version.
     */
    val chartVersion: Property<String>


    /**
     * The directory that contains the chart sources.
     */
    val sourceDir: DirectoryProperty
}


private open class DefaultHelmChart
@Inject constructor(
    private val name: String,
    project: Project
) : HelmChart {

    override fun getName(): String =
        name


    override val chartName: Property<String> =
        project.objects.property<String>()
            .convention(name)


    override val chartVersion: Property<String> =
        project.objects.property<String>()
            .convention(project.versionProvider)


    override val sourceDir: DirectoryProperty =
        project.objects.directoryProperty()


    override fun getBuildDependencies(): TaskDependency =
        TaskDependency { task ->
            if (task != null) {
                setOf(
                    task.project.tasks.getByName(packageTaskName)
                )
            } else {
                emptySet()
            }
        }
}


/**
 * Creates a [NamedDomainObjectContainer] that holds [HelmChart]s.
 *
 * @param project the Gradle [Project]
 * @return the container for `HelmChart`s
 */
internal fun helmChartContainer(project: Project): NamedDomainObjectContainer<HelmChart> =
    project.container(HelmChart::class.java) { name ->
        project.objects.newInstance(DefaultHelmChart::class.java, name, project)
    }
