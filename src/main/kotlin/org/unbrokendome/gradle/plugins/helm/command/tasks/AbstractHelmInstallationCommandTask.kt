package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.unbrokendome.gradle.plugins.helm.command.ConfigurableHelmInstallFromRepositoryOptions
import org.unbrokendome.gradle.plugins.helm.command.ConfigurableHelmValueOptions
import org.unbrokendome.gradle.plugins.helm.command.HelmExecProviderSupport
import org.unbrokendome.gradle.plugins.helm.command.HelmInstallFromRepositoryOptionsApplier
import org.unbrokendome.gradle.plugins.helm.command.HelmInstallationOptionsApplier
import org.unbrokendome.gradle.plugins.helm.command.HelmValueOptionsApplier
import java.io.File
import java.net.URI


@Suppress("LeakingThis")
abstract class AbstractHelmInstallationCommandTask :
    AbstractHelmServerOperationCommandTask(),
    ConfigurableHelmInstallFromRepositoryOptions,
    ConfigurableHelmValueOptions {

    /**
     * Release name.
     */
    @get:Input
    abstract val releaseName: Property<String>


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
    abstract val chart: Property<String>


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
     * Chart repository URL where to locate the requested chart.
     *
     * Corresponds to the `--repo` Helm CLI parameter.
     *
     * Use this when the [chart] property contains only a simple chart reference, without a symbolic repository name.
     */
    @get:[Input Optional]
    abstract override val repository: Property<URI>


    init {
        devel.convention(false)

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
