package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.Buildable
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
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
    project: Project,
    objects: ObjectFactory
) : HelmChart {

    final override fun getName(): String =
        name


    final override val chartName: Property<String> =
        objects.property<String>()
            .convention(name)


    final override val chartVersion: Property<String> =
        objects.property<String>()
            .convention(project.versionProvider)


    final override val sourceDir: DirectoryProperty =
        objects.directoryProperty()


    final override fun getBuildDependencies(): TaskDependency =
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
 * @receiver the Gradle [Project]
 * @return the container for `HelmChart`s
 */
internal fun Project.helmChartContainer(): NamedDomainObjectContainer<HelmChart> =
    container(HelmChart::class.java) { name ->
        objects.newInstance(DefaultHelmChart::class.java, name, this)
    }
