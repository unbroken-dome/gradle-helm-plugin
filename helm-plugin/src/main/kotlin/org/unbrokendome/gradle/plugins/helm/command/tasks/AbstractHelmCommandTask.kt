package com.citi.gradle.plugins.helm.command.tasks

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.workers.WorkerExecutor
import com.citi.gradle.plugins.helm.HELM_GROUP
import com.citi.gradle.plugins.helm.command.GlobalHelmOptions
import com.citi.gradle.plugins.helm.command.HelmExecProviderSupport
import com.citi.gradle.plugins.helm.command.HelmExecSpec
import com.citi.gradle.plugins.helm.command.internal.GlobalHelmOptionsApplier
import org.unbrokendome.gradle.pluginutils.property
import org.unbrokendome.gradle.pluginutils.withDefault
import javax.inject.Inject


/**
 * Base class for tasks that invoke a Helm CLI command.
 */
abstract class AbstractHelmCommandTask
    : DefaultTask(), GlobalHelmOptions {

    init {
        group = HELM_GROUP
    }


    @get:Inject
    internal open val workerExecutor: WorkerExecutor
        get() = throw UnsupportedOperationException()


    @get:Internal("represented by other properties")
    internal val globalOptions: Property<GlobalHelmOptions> =
        project.objects.property()


    @get:Input
    final override val executable: Provider<String>
            get() = localExecutable
                .withDefault(downloadedExecutable, project.providers)
                .withDefault("helm", project.providers)


    @get:Internal
    internal val localExecutable: Provider<String> =
        globalOptions.flatMap { it.executable }


    @get:Internal
    internal val downloadedExecutable: Property<String> =
        project.objects.property()


    @get:Console
    final override val debug: Provider<Boolean>
        get() = globalOptions.flatMap { it.debug }


    @get:Input
    final override val extraArgs: Provider<List<String>>
        get() = globalOptions.flatMap { it.extraArgs }


    @get:Internal
    final override val xdgDataHome: Provider<Directory>
        get() = globalOptions.flatMap { it.xdgDataHome }


    @get:Internal
    final override val xdgConfigHome: Provider<Directory>
        get() = globalOptions.flatMap { it.xdgConfigHome }


    @get:Internal
    final override val xdgCacheHome: Provider<Directory>
        get() = globalOptions.flatMap { it.xdgCacheHome }


    @get:Internal
    protected val registryConfigFile: Provider<RegularFile>
        get() = xdgConfigHome.map { it.file("helm/registry.json") }


    @get:Internal
    protected val repositoryCacheDir: Provider<Directory>
        get() = xdgCacheHome.map { it.dir("helm/repository") }


    @get:Internal
    protected val repositoryConfigFile: Provider<RegularFile>
        get() = xdgConfigHome.map { it.file("helm/repositories.yaml") }


    protected fun execHelm(
        command: String, subcommand: String? = null, action: (HelmExecSpec.() -> Unit)? = null
    ) {
        execProviderSupport.execHelm(command, subcommand, action?.let { Action(it) })
    }


    protected fun execHelmCaptureOutput(
        command: String, subcommand: String? = null, action: (HelmExecSpec.() -> Unit)? = null
    ): String =
        execProviderSupport.execHelmCaptureOutput(command, subcommand, action?.let { Action(it) })


    @get:Internal
    internal open val execProviderSupport: HelmExecProviderSupport
        get() = HelmExecProviderSupport(project, workerExecutor, this, GlobalHelmOptionsApplier)
}
