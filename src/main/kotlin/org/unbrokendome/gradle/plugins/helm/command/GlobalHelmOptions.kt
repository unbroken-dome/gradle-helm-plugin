package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.slf4j.LoggerFactory
import org.unbrokendome.gradle.plugins.helm.util.ifPresent


/**
 * Holds options that apply to all Helm commands.
 */
interface GlobalHelmOptions : HelmOptions {

    /**
     * The name or path of the Helm executable. The `PATH` variable is taken into account, so this
     * can just be `"helm"` if the Helm client is installed in a suitable location.
     */
    val executable: Provider<String>

    /**
     * Indicates whether to use the verbose output (`--debug` flag) when invoking commands.
     */
    val debug: Provider<Boolean>

    /**
     * Additional command-line arguments to pass to the Helm CLI.
     *
     * This can be used for command-line options that have no counterpart in the plugin.
     */
    val extraArgs: ListProperty<String>

    /**
     * Base directory for storing data.
     *
     * Corresponds to the `XDG_DATA_HOME` environment variable.
     * If not set, defaults to `helm/data` inside the project build directory.
     *
     * See [https://helm.sh/docs/helm/helm/] for details about how XDG base directories are used by the Helm CLI.
     */
    val xdgDataHome: DirectoryProperty

    /**
     * Base directory for storing configuration.
     *
     * Corresponds to the `XDG_CONFIG_HOME` environment variable.
     * If not set, defaults to `helm/config` inside the project build directory.
     *
     * See [https://helm.sh/docs/helm/helm/] for details about how XDG base directories are used by the Helm CLI.
     */
    val xdgConfigHome: DirectoryProperty

    /**
     * Base directory for storing cached data.
     *
     * Corresponds to the `XDG_CACHE_HOME` environment variable.
     * If not set, defaults to `.gradle/helm/cache` inside the _root_ project directory.
     *
     * See [https://helm.sh/docs/helm/helm/] for details about how XDG base directories are used by the Helm CLI.
     */
    val xdgCacheHome: DirectoryProperty
}



internal object GlobalHelmOptionsApplier : HelmOptionsApplier {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun apply(spec: HelmExecSpec, options: HelmOptions) {
        if (options is GlobalHelmOptions) {

            logger.debug("Applying GlobalHelmOptions: {}", options)

            with(spec) {
                withExecSpec {
                    executable = options.executable.getOrElse("helm")

                    options.extraArgs.ifPresent { extraArgs ->
                        args(extraArgs)
                    }
                }
                flag("--debug", options.debug)

                environment("XDG_DATA_HOME", options.xdgDataHome)
                environment("XDG_CONFIG_HOME", options.xdgConfigHome)
                environment("XDG_CACHE_HOME", options.xdgCacheHome)
            }
        }
    }
}
