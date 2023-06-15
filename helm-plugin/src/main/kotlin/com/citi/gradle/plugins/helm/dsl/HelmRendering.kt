package com.citi.gradle.plugins.helm.dsl

import org.gradle.api.Buildable
import org.gradle.api.Named
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskDependency
import com.citi.gradle.plugins.helm.command.ConfigurableHelmValueOptions
import com.citi.gradle.plugins.helm.command.internal.HelmValueOptionsHolder
import com.citi.gradle.plugins.helm.rules.renderTaskName
import org.unbrokendome.gradle.pluginutils.listProperty
import org.unbrokendome.gradle.pluginutils.property
import javax.inject.Inject


/**
 * Represents a local rendering of templates for the containing chart.
 */
interface HelmRendering : Named, Buildable, ConfigurableHelmValueOptions {

    companion object {

        @JvmStatic
        val DEFAULT_RENDERING_NAME = "default"
    }

    /**
     * The name of the release.
     */
    val releaseName: Property<String>

    /**
     * Kubernetes API versions used for `Capabilities.APIVersions`.
     *
     * Corresponds to the `--api-versions` CLI option.
     */
    val apiVersions: ListProperty<String>

    /**
     * If `true`, set `Release.IsUpgrade` instead of `Release.IsInstall`.
     *
     * Corresponds to the `--is-upgrade` CLI option.
     */
    val isUpgrade: Property<Boolean>

    /**
     * If not empty, only output manifests rendered from the given templates.
     *
     * Corresponds to the `--show-only` CLI option.
     */
    val showOnly: ListProperty<String>

    /**
     * If `true`, use the release name in the output path.
     *
     * Corresponds to the `--release-name` CLI option.
     */
    val useReleaseNameInOutputPath: Property<Boolean>

    /**
     * If `true`, validate your manifests against the Kubernetes cluster you are currently pointing at.
     * This is the same validation performed on an install.
     *
     * Corresponds to the `--validate` CLI option.
     */
    val validate: Property<Boolean>

    /**
     * Directory into which the template output files will be written.
     *
     * Corresponds to the `--output-dir` CLI option.
     */
    val outputDir: DirectoryProperty
}


private open class DefaultHelmRendering
@Inject constructor(
    private val name: String,
    private val chartName: String,
    objects: ObjectFactory
) : HelmRendering, ConfigurableHelmValueOptions by HelmValueOptionsHolder(objects) {

    final override fun getName(): String =
        name

    final override val releaseName: Property<String> =
        objects.property()

    final override val apiVersions: ListProperty<String> =
        objects.listProperty()

    final override val isUpgrade: Property<Boolean> =
        objects.property()

    final override val showOnly: ListProperty<String> =
        objects.listProperty()

    final override val validate: Property<Boolean> =
        objects.property()

    final override val useReleaseNameInOutputPath: Property<Boolean> =
        objects.property()

    final override val outputDir: DirectoryProperty =
        objects.directoryProperty()

    final override fun getBuildDependencies(): TaskDependency {
        return TaskDependency { task ->
            val taskName = renderTaskName(chartName, name)
            setOfNotNull(
                task?.project?.tasks?.getByName(taskName)
            )
        }
    }
}


internal fun ObjectFactory.createHelmRendering(name: String, chartName: String): HelmRendering =
    newInstance(DefaultHelmRendering::class.java, name, chartName)
