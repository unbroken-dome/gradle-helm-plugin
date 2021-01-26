package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.slf4j.LoggerFactory
import org.unbrokendome.gradle.pluginutils.ifPresent


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


internal fun ConfigurableGlobalHelmOptions.conventionsFrom(source: GlobalHelmOptions) = apply {
    executable.convention(source.executable)
    extraArgs.addAll(source.extraArgs)
    xdgDataHome.convention(source.xdgDataHome)
    xdgConfigHome.convention(source.xdgConfigHome)
    xdgCacheHome.convention(source.xdgCacheHome)
}


internal class DelegateGlobalHelmOptions(
    private val provider: Provider<GlobalHelmOptions>
) : GlobalHelmOptions {

    override val executable: Provider<String>
        get() = provider.flatMap { it.executable }

    override val debug: Provider<Boolean>
        get() = provider.flatMap { it.debug }

    override val extraArgs: Provider<List<String>>
        get() = provider.flatMap { it.extraArgs }

    override val xdgDataHome: Provider<Directory>
        get() = provider.flatMap { it.xdgDataHome }

    override val xdgConfigHome: Provider<Directory>
        get() = provider.flatMap { it.xdgConfigHome }

    override val xdgCacheHome: Provider<Directory>
        get() = provider.flatMap { it.xdgCacheHome }
}


internal object GlobalHelmOptionsApplier : HelmOptionsApplier {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun apply(spec: HelmExecSpec, options: HelmOptions) {
        if (options is GlobalHelmOptions) {

            logger.debug("Applying options: {}", options)

            with(spec) {

                executable(options.executable.getOrElse("helm"))

                flag("--debug", options.debug)

                options.extraArgs.ifPresent { extraArgs ->
                    args(extraArgs)
                }

                environment("XDG_DATA_HOME", options.xdgDataHome)
                environment("XDG_CONFIG_HOME", options.xdgConfigHome)
                environment("XDG_CACHE_HOME", options.xdgCacheHome)
            }
        }
    }
}
