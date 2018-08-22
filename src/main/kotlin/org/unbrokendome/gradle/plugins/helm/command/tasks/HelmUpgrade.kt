package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.unbrokendome.gradle.plugins.helm.command.valuesOptions
import org.unbrokendome.gradle.plugins.helm.util.MapProperty
import org.unbrokendome.gradle.plugins.helm.util.mapProperty
import org.unbrokendome.gradle.plugins.helm.util.property
import java.io.File
import java.net.URI


/**
 * Upgrades a release on the cluster. Corresponds to the `helm upgrade` CLI command.
 */
open class HelmUpgrade : AbstractHelmServerCommandTask() {

    /**
     * Name of the release to be upgraded.
     */
    @get:Input
    val releaseName: Property<String> =
            project.objects.property()


    /**
     * The chart to be installed. This can be any of the forms accepted by the Helm CLI.
     *
     * - chart reference (`String`): e.g. `stable/mariadb`
     * - path to a packaged chart (`String`, [File], [RegularFile])
     * - path to an unpacked chart directory (`String`, [File], [Directory])
     * - absolute URL (`String`, [URI]): e.g. `https://example.com/charts/nginx-1.2.3.tgz`
     * - chart reference and repository URL
     */
    @get:Input
    val chart: Property<Any> =
            project.objects.property()


    /**
     * If `true`, simulate an upgrade.
     */
    @get:Internal
    val dryRun: Property<Boolean> =
            project.objects.property()


    /**
     * If `true`, run an install if a release by this name doesn't already exist.
     */
    @get:Internal
    val install: Property<Boolean> =
            project.objects.property()


    /**
     * Namespace to install the release into (only used if [install] is set).
     * Defaults to the current kube config namespace.
     */
    @get:Internal
    val namespace: Property<String> =
            project.objects.property()


    /**
     * If `true`, performs pods restart for the resource if applicable.
     */
    @get:Internal
    val recreatePods: Property<Boolean> =
            project.objects.property()


    /**
     * If `true`, reset the values to the ones built into the chart when upgrading.
     */
    @get:Internal
    val resetValues: Property<Boolean> =
            project.objects.property()


    /**
     * If `true`, reuse the last release's values, and merge in any new values. If [resetValues] is specified,
     * this is ignored.
     */
    @get:Internal
    val reuseValues: Property<Boolean> =
            project.objects.property()


    /**
     * Time in seconds to wait for any individual Kubernetes operation (like Jobs for hooks). Default is 300.
     */
    @get:Internal
    val timeoutSeconds: Property<Int> =
            project.objects.property()


    /**
     * Values to be used for the release.
     */
    @get:Input
    val values: MapProperty<String, Any> =
            mapProperty()


    /**
     * A collection of YAML files containing values for this release.
     */
    @get:InputFiles
    val valueFiles: ConfigurableFileCollection =
            project.layout.configurableFiles()


    /**
     * Specify the exact chart version to use. If this is not specified, the latest version is used.
     */
    @get:Internal
    val version: Property<String> =
            project.objects.property()


    /**
     * If `true`, will wait until all Pods, PVCs, Services, and minimum number of Pods of a Deployment are in a ready
     * state before marking the release as successful. It will wait for as along as [timeoutSeconds].
     */
    @get:Internal
    val wait: Property<Boolean> =
            project.objects.property()


    @TaskAction
    fun upgradeRelease() {
        execHelm("upgrade") {
            flag("--install", install)
            option("--namespace", namespace)
            option("--version", version)
            valuesOptions(values, valueFiles)
            flag("--recreate-pods", recreatePods)
            flag("--reset-values", resetValues)
            flag("--reuse-values", reuseValues)
            flag("--wait", wait)
            flag("--dry-run", dryRun)
            args(releaseName)
            args(chart)
        }
    }
}
