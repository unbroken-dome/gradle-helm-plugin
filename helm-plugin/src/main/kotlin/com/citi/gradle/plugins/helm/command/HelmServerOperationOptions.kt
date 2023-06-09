package com.citi.gradle.plugins.helm.command

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import java.time.Duration


interface HelmServerOperationOptions : HelmServerOptions {

    val dryRun: Provider<Boolean>

    val noHooks: Provider<Boolean>

    val remoteTimeout: Provider<Duration>
}


interface ConfigurableHelmServerOperationOptions : ConfigurableHelmServerOptions, HelmServerOperationOptions {

    /**
     * If `true`, only simulate the operation.
     *
     * Corresponds to the `--dry-run` CLI parameter.
     */
    override val dryRun: Property<Boolean>


    /**
     * If `true`, prevent hooks from running during the operation.
     *
     * Corresponds to the `--no-hooks` CLI parameter.
     */
    override val noHooks: Property<Boolean>


    /**
     * Time to wait for any individual Kubernetes operation (like Jobs for hooks). Default is 300.
     *
     * Corresponds to the `--timeout` command line option in the Helm CLI.
     */
    override val remoteTimeout: Property<Duration>
}


