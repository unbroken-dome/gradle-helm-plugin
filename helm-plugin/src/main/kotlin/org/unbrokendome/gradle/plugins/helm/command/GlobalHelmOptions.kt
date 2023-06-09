package com.citi.gradle.plugins.helm.command

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider


interface GlobalHelmOptions : HelmOptions {

    val executable: Provider<String>

    val debug: Provider<Boolean>

    val extraArgs: Provider<List<String>>

    val xdgDataHome: Provider<Directory>

    val xdgConfigHome: Provider<Directory>

    val xdgCacheHome: Provider<Directory>
}


/**
 * Holds options that apply to all Helm commands.
 */
interface ConfigurableGlobalHelmOptions : GlobalHelmOptions, ConfigurableHelmOptions {

    /**
     * The name or path of the Helm executable. The `PATH` variable is taken into account, so this
     * can just be `"helm"` if the Helm client is installed in a suitable location.
     */
    override val executable: Property<String>

    /**
     * Indicates whether to use the verbose output (`--debug` flag) when invoking commands.
     */
    override val debug: Property<Boolean>

    /**
     * Additional command-line arguments to pass to the Helm CLI.
     *
     * This can be used for command-line options that have no counterpart in the plugin.
     */
    override val extraArgs: ListProperty<String>

    /**
     * Base directory for storing data.
     *
     * Corresponds to the `XDG_DATA_HOME` environment variable.
     * If not set, defaults to `helm/data` inside the project build directory.
     *
     * See [https://helm.sh/docs/helm/helm/] for details about how XDG base directories are used by the Helm CLI.
     */
    override val xdgDataHome: DirectoryProperty

    /**
     * Base directory for storing configuration.
     *
     * Corresponds to the `XDG_CONFIG_HOME` environment variable.
     * If not set, defaults to `helm/config` inside the project build directory.
     *
     * See [https://helm.sh/docs/helm/helm/] for details about how XDG base directories are used by the Helm CLI.
     */
    override val xdgConfigHome: DirectoryProperty

    /**
     * Base directory for storing cached data.
     *
     * Corresponds to the `XDG_CACHE_HOME` environment variable.
     * If not set, defaults to `.gradle/helm/cache` inside the _root_ project directory.
     *
     * See [https://helm.sh/docs/helm/helm/] for details about how XDG base directories are used by the Helm CLI.
     */
    override val xdgCacheHome: DirectoryProperty
}
