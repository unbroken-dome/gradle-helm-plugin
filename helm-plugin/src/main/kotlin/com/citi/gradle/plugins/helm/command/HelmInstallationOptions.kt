package com.citi.gradle.plugins.helm.command

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider


interface HelmInstallationOptions : HelmServerOperationOptions {

    val atomic: Provider<Boolean>

    val devel: Provider<Boolean>

    val verify: Provider<Boolean>

    val wait: Provider<Boolean>

    val waitForJobs: Provider<Boolean>

    val version: Provider<String>

    val createNamespace: Provider<Boolean>
}


interface ConfigurableHelmInstallationOptions : ConfigurableHelmServerOperationOptions, HelmInstallationOptions {

    /**
     * If `true`, roll back changes on failure.
     *
     * Corresponds to the `--atomic` Helm CLI parameter.
     */
    override val atomic: Property<Boolean>


    /**
     * If `true`, use development versions, too. Equivalent to version `>0.0.0-0`.
     *
     * Corresponds to the `--devel` CLI parameter.
     */
    override val devel: Property<Boolean>


    /**
     * If `true`, verify the package before installing it.
     *
     * Corresponds to the `--verify` CLI parameter.
     */
    override val verify: Property<Boolean>


    /**
     * If `true`, use the `--wait` flag when installing/upgrading or uninstalling this release.
     *
     * When installing or upgrading, it will wait until all Pods, PVCs, Services, and minimum
     * number of Pods of a Deployment are in a ready state before marking the release as successful.
     * When uninstalling, will wait until all the resources are deleted before returning.
     * It will wait for as long as [remoteTimeout].
     */
    override val wait: Property<Boolean>


    /**
     * If `true`, and [wait] is also `true`, will wait until all Jobs have been completed before
     * marking the release as successful. It will wait for as long as [remoteTimeout].
     */
    override val waitForJobs: Property<Boolean>


    /**
     * Specify the exact chart version to install. If this is not specified, the latest version is installed.
     *
     * Corresponds to the `--version` CLI parameter.
     */
    override val version: Property<String>


    /**
     * If `true`, create the release namespace if not present.
     *
     * Corresponds to the `--create-namespace` CLI parameter.
     */
    override val createNamespace: Property<Boolean>
}
