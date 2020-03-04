package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.unbrokendome.gradle.plugins.helm.command.valuesOptions
import org.unbrokendome.gradle.plugins.helm.util.mapProperty
import org.unbrokendome.gradle.plugins.helm.util.property
import java.io.File
import java.net.URI


/**
 * Installs a chart into the cluster. Corresponds to the `helm install` CLI command.
 */
open class HelmInstall : AbstractHelmServerCommandTask() {

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
     * Chart repository URL where to locate the requested chart. Corresponds to the `--repo` Helm CLI parameter.
     *
     * Use this when the [chart] property contains only a simple chart reference, without a symbolic repository name.
     */
    @get:[Input Optional]
    val repository: Property<URI> =
        project.objects.property()


    /**
     * If `true`, simulate an install.
     */
    @get:Internal
    val dryRun: Property<Boolean> =
        project.objects.property()


    /**
     * Release name. If unspecified, Helm will auto-generate a name.
     */
    @get:Internal
    val releaseName: Property<String> =
        project.objects.property()


    /**
     * Namespace to install the release into. Defaults to the current kube config namespace.
     */
    @get:Internal
    val namespace: Property<String> =
        project.objects.property()


    /**
     * If `true`, re-use the given release name, even if that name is already used.
     */
    @get:Internal
    val replace: Property<Boolean> =
        project.objects.property()


    /**
     * Values to be used for the release.
     */
    @get:Input
    val values: MapProperty<String, Any> =
        project.objects.mapProperty()


    /**
     * A collection of YAML files containing values for this release.
     */
    @get:InputFiles
    val valueFiles: ConfigurableFileCollection =
        project.objects.fileCollection()


    /**
     * Specify the exact chart version to install. If this is not specified, the latest version is installed.
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
    fun install() {
        execHelm("install") {

            option("--name", releaseName)
            option("--version", version)
            option("--namespace", namespace)

            valuesOptions(values, valueFiles)

            flag("--replace", replace)
            flag("--dry-run", dryRun)

            flag("--wait", wait)

            args(chart)
        }
    }
}
