package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.slf4j.LoggerFactory
import java.net.URI


internal interface HelmInstallationOptions : HelmServerOptions, HelmValueOptions {

    /**
     * Release name.
     */
    val releaseName: Property<String>


    /**
     * The chart to be installed. This can be any of the forms accepted by the Helm CLI.
     *
     * - chart reference: e.g. `stable/mariadb`
     * - path to a packaged chart
     * - path to an unpacked chart directory
     * - absolute URL: e.g. `https://example.com/charts/nginx-1.2.3.tgz`
     * - simple chart reference, e.g. `mariadb` (you must also set the [repository] property in this case)
     */
    val chart: Property<String>


    /**
     * If `true`, roll back changes on failure.
     *
     * Corresponds to the `--atomic` Helm CLI parameter.
     */
    @get:Internal
    val atomic: Property<Boolean>


    /**
     * Verify certificates of HTTPS-enabled servers using this CA bundle.
     *
     * Corresponds to the `--ca-file` CLI parameter.
     */
    @get:Internal
    val caFile: RegularFileProperty


    /**
     * Identify HTTPS client using this SSL certificate file.
     *
     * Corresponds to the `--cert-file` CLI parameter.
     */
    @get:Internal
    val certFile: RegularFileProperty


    /**
     * If `true`, use development versions, too. Equivalent to version `>0.0.0-0`.
     * If [version] is set, this is ignored.
     *
     * Corresponds to the `--devel` CLI parameter.
     */
    @get:Input
    val devel: Property<Boolean>


    /**
     * If `true`, simulate an install.
     *
     * Corresponds to the `--dry-run` CLI parameter.
     */
    @get:Internal
    val dryRun: Property<Boolean>


    /**
     * Identify HTTPS client using this SSL key file.
     *
     * Corresponds to the `--key-file` CLI parameter.
     */
    @get:Internal
    val keyFile: RegularFileProperty


    /**
     * If `true`, prevent hooks from running during the operation.
     *
     * Corresponds to the `--no-hooks` CLI parameter.
     */
    @get:Internal
    val noHooks: Property<Boolean>


    /**
     * Chart repository password where to locate the requested chart.
     *
     * Corresponds to the `--password` CLI parameter.
     */
    @get:Internal
    val password: Property<String>


    /**
     * Chart repository URL where to locate the requested chart.
     *
     * Corresponds to the `--repo` Helm CLI parameter.
     *
     * Use this when the [chart] property contains only a simple chart reference, without a symbolic repository name.
     */
    @get:[Input Optional]
    val repository: Property<URI>


    /**
     * Chart repository username where to locate the requested chart.
     *
     * Corresponds to the `--username` CLI parameter.
     */
    @get:Internal
    val username: Property<String>


    /**
     * If `true`, verify the package before installing it.
     *
     * Corresponds to the `--verify` CLI parameter.
     */
    @get:Internal
    val verify: Property<Boolean>


    /**
     * Specify the exact chart version to install. If this is not specified, the latest version is installed.
     *
     * Corresponds to the `--version` Helm CLI parameter.
     */
    @get:Internal
    val version: Property<String>


    /**
     * If `true`, will wait until all Pods, PVCs, Services, and minimum number of Pods of a Deployment are in a ready
     * state before marking the release as successful. It will wait for as along as [remoteTimeout].
     */
    @get:Internal
    val wait: Property<Boolean>
}


internal object HelmInstallationOptionsApplier : HelmOptionsApplier {

    private val logger = LoggerFactory.getLogger(javaClass)


    override fun apply(spec: HelmExecSpec, options: HelmOptions) {
        if (options is HelmInstallationOptions) {

            logger.debug("Applying HelmInstallationOptions: {}", options)

            with(spec) {

                args(options.releaseName)
                args(options.chart)

                flag("--atomic", options.atomic)
                option("--ca-file", options.caFile)
                option("--cert-file", options.certFile)
                flag("--devel", options.devel)
                flag("--dry-run", options.dryRun)
                option("--key-file", options.keyFile)
                flag("--no-hooks", options.noHooks)
                option("--password", options.password)
                option("--repo", options.repository)
                option("--username", options.username)
                flag("--verify", options.verify)
                option("--version", options.version)
                flag("--wait", options.wait)
            }
        }
    }

    override val implies: List<HelmOptionsApplier>
        get() = listOf(HelmServerOptionsApplier, HelmValueOptionsApplier)
}
