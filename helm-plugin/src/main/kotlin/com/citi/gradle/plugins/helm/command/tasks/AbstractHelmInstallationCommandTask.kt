package com.citi.gradle.plugins.helm.command.tasks

import java.io.File
import java.net.URI
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import com.citi.gradle.plugins.helm.command.ConfigurableHelmInstallFromRepositoryOptions
import com.citi.gradle.plugins.helm.command.ConfigurableHelmValueOptions
import com.citi.gradle.plugins.helm.command.HelmExecProviderSupport
import com.citi.gradle.plugins.helm.command.internal.HelmInstallFromRepositoryOptionsApplier
import com.citi.gradle.plugins.helm.command.internal.HelmInstallationOptionsApplier
import com.citi.gradle.plugins.helm.command.internal.HelmValueOptionsApplier
import org.unbrokendome.gradle.pluginutils.mapProperty
import org.unbrokendome.gradle.pluginutils.property


abstract class AbstractHelmInstallationCommandTask :
    AbstractHelmServerOperationCommandTask(),
    ConfigurableHelmInstallFromRepositoryOptions,
    ConfigurableHelmValueOptions {

    /**
     * Release name.
     */
    @get:Input
    open val releaseName: Property<String> =
        project.objects.property()


    /**
     * The chart to be installed. This can be any of the forms accepted by the Helm CLI.
     *
     * - chart reference: e.g. `stable/mariadb`
     * - path to a packaged chart
     * - path to an unpacked chart directory
     * - absolute URL: e.g. `https://example.com/charts/nginx-1.2.3.tgz`
     * - simple chart reference, e.g. `mariadb` (you must also set the [repository] property in this case)
     */
    @get:Input
    val chart: Property<String> =
        project.objects.property()


    /**
     * Specify the exact chart version to install. If this is not specified, the latest version is installed.
     *
     * Corresponds to the `--version` Helm CLI parameter.
     */
    @get:[Input Optional]
    override val version: Property<String> =
        project.objects.property()


    /**
     * Sets the chart to be installed. The value can be any of the forms accepted by the Helm CLI.
     *
     * This is a convenience method that can be used instead of setting the [chart] property directly.
     *
     * The following argument types are accepted:
     *
     * - A chart reference (`String`): e.g. `stable/mariadb`.
     * - A path to a packaged chart (`String`, [File], [RegularFile])
     * - A path to an unpacked chart directory (`String`, [File], [Directory])
     * - An absolute URL (`String`, [URI]): e.g. `https://example.com/charts/nginx-1.2.3.tgz`
     * - A simple chart reference (`String`), e.g. `mariadb`.
     *   Note that you must also set the [repository] property in this case.
     * - a [Provider] of any of the above.
     *
     */
    fun from(chart: Any) {
        if (chart is Provider<*>) {
            this.chart.set(chart.map { it.toString() })
        } else {
            this.chart.set(chart.toString())
        }
    }


    /**
     * If `true`, roll back changes on failure.
     *
     * Corresponds to the `--atomic` Helm CLI parameter.
     */
    @get:Internal
    final override val atomic: Property<Boolean> =
        project.objects.property()


    /**
     * Verify certificates of HTTPS-enabled servers using this CA bundle.
     *
     * Corresponds to the `--ca-file` CLI parameter.
     */
    @get:Internal
    final override val caFile: RegularFileProperty =
        project.objects.fileProperty()


    /**
     * Identify HTTPS client using this SSL certificate file.
     *
     * Corresponds to the `--cert-file` CLI parameter.
     */
    @get:Internal
    final override val certFile: RegularFileProperty =
        project.objects.fileProperty()


    /**
     * If `true`, use development versions, too. Equivalent to version `>0.0.0-0`.
     * If [version] is set, this is ignored.
     *
     * Corresponds to the `--devel` CLI parameter.
     */
    @get:[Input Optional]
    final override val devel: Property<Boolean> =
        project.objects.property<Boolean>()
            .convention(false)


    /**
     * Identify HTTPS client using this SSL key file.
     *
     * Corresponds to the `--key-file` CLI parameter.
     */
    @get:Internal
    final override val keyFile: RegularFileProperty =
        project.objects.fileProperty()


    /**
     * Chart repository password where to locate the requested chart.
     *
     * Corresponds to the `--password` CLI parameter.
     */
    @get:Internal
    final override val password: Property<String> =
        project.objects.property()


    /**
     * Chart repository URL where to locate the requested chart.
     *
     * Corresponds to the `--repo` Helm CLI parameter.
     *
     * Use this when the [chart] property contains only a simple chart reference, without a symbolic repository name.
     */
    @get:[Input Optional]
    final override val repository: Property<URI> =
        project.objects.property()


    /**
     * Chart repository username where to locate the requested chart.
     *
     * Corresponds to the `--username` CLI parameter.
     */
    @get:Internal
    final override val username: Property<String> =
        project.objects.property()


    /**
     * Values to be used for the release.
     *
     * Entries in the map will be sent to the CLI using either the `--set-string` option (for strings) or the
     * `--set` option (for all other types).
     */
    @get:Input
    final override val values: MapProperty<String, Any> =
        project.objects.mapProperty()


    /**
     * Values read from the contents of files, to be used for the release.
     *
     * Corresponds to the `--set-file` CLI option.
     *
     * The values of the map can be of any type that is accepted by [Project.file]. Additionally, when adding a
     * [Provider] that represents an output file of another task, this task will automatically have a task
     * dependency on the producing task.
     *
     * Not to be confused with [valueFiles], which contains a collection of YAML files that supply multiple values.
     */
    @get:Input
    final override val fileValues: MapProperty<String, Any> =
        project.objects.mapProperty()


    /**
     * A collection of YAML files containing values for this release.
     *
     * Corresponds to the `--values` CLI option.
     *
     * Not to be confused with [fileValues], which contains entries whose values are the contents of files.
     */
    @get:InputFiles
    final override val valueFiles: ConfigurableFileCollection = project.objects.fileCollection()


    /**
     * If `true`, verify the package before installing it.
     *
     * Corresponds to the `--verify` CLI parameter.
     */
    @get:Internal
    final override val verify: Property<Boolean> =
        project.objects.property()


    /**
     * If `true`, will wait until all Pods, PVCs, Services, and minimum number of Pods of a Deployment are in a ready
     * state before marking the release as successful. It will wait for as along as [remoteTimeout].
     */
    @get:Internal
    final override val wait: Property<Boolean> =
        project.objects.property()


    /**
     * If `true`, and [wait] is also `true`, will wait until all Jobs have been completed before
     * marking the release as successful. It will wait for as long as [remoteTimeout].
     */
    @get:Internal
    final override val waitForJobs: Property<Boolean> =
        project.objects.property()


    /**
     * If `true`, create the release namespace if not present.
     *
     * Corresponds to the `--create-namespace` CLI parameter.
     */
    @get:Internal
    final override val createNamespace: Property<Boolean> =
        project.objects.property()


    init {
        inputs.files(
            fileValues.keySet().map { keys ->
                keys.map { fileValues.getting(it) }
            }
        )
    }


    override val execProviderSupport: HelmExecProviderSupport
        get() = super.execProviderSupport.addOptionsAppliers(
            HelmInstallationOptionsApplier, HelmInstallFromRepositoryOptionsApplier, HelmValueOptionsApplier
        )
}
