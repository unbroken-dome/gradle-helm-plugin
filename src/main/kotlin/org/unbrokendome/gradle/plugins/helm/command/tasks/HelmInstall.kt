package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.unbrokendome.gradle.plugins.helm.command.valuesOptions
import org.unbrokendome.gradle.plugins.helm.util.MapProperty
import org.unbrokendome.gradle.plugins.helm.util.emptyProperty
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
     * If `true`, simulate an install.
     */
    @get:Internal
    val dryRun: Property<Boolean> =
            project.objects.emptyProperty()


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
            project.objects.emptyProperty()


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
            project.objects.emptyProperty()


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
