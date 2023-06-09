package com.citi.gradle.plugins.helm.command.internal

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.slf4j.LoggerFactory
import com.citi.gradle.plugins.helm.command.ConfigurableGlobalHelmOptions
import com.citi.gradle.plugins.helm.command.GlobalHelmOptions
import com.citi.gradle.plugins.helm.command.HelmExecSpec
import com.citi.gradle.plugins.helm.command.HelmOptions
import org.unbrokendome.gradle.pluginutils.ifPresent


fun ConfigurableGlobalHelmOptions.conventionsFrom(source: GlobalHelmOptions) = apply {
    executable.convention(source.executable)
    extraArgs.addAll(source.extraArgs)
    xdgDataHome.convention(source.xdgDataHome)
    xdgConfigHome.convention(source.xdgConfigHome)
    xdgCacheHome.convention(source.xdgCacheHome)
}


class DelegateGlobalHelmOptions(
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


object GlobalHelmOptionsApplier : HelmOptionsApplier {

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
