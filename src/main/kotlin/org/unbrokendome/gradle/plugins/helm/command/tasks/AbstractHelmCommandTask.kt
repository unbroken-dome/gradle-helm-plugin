package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.process.ExecResult
import org.unbrokendome.gradle.plugins.helm.HELM_GROUP
import org.unbrokendome.gradle.plugins.helm.command.GlobalHelmOptions
import org.unbrokendome.gradle.plugins.helm.command.GlobalHelmOptionsApplier
import org.unbrokendome.gradle.plugins.helm.command.HelmExecProviderSupport
import org.unbrokendome.gradle.plugins.helm.command.HelmExecSpec
import org.unbrokendome.gradle.plugins.helm.command.execHelm
import org.unbrokendome.gradle.plugins.helm.util.listProperty
import org.unbrokendome.gradle.plugins.helm.util.property


/**
 * Base class for tasks that invoke a Helm CLI command.
 */
abstract class AbstractHelmCommandTask : DefaultTask(), GlobalHelmOptions {

    init {
        group = HELM_GROUP
    }


    @get:Input
    final override val executable: Property<String> =
        project.objects.property<String>()
            .convention("helm")


    @get:Console
    final override val debug: Property<Boolean> =
        project.objects.property<Boolean>()
            .convention(false)


    @get:Input
    final override val extraArgs: ListProperty<String> =
        project.objects.listProperty()


    @get:Internal
    final override val xdgDataHome: DirectoryProperty =
        project.objects.directoryProperty()
            .convention(project.layout.projectDirectory.dir("${project.rootDir}/.gradle/helm/data"))


    @get:Internal
    final override val xdgConfigHome: DirectoryProperty =
        project.objects.directoryProperty()
            .convention(project.layout.projectDirectory.dir("${project.rootDir}/.gradle/helm/config"))


    @get:Internal
    final override val xdgCacheHome: DirectoryProperty =
        project.objects.directoryProperty()
            .convention(project.layout.projectDirectory.dir("${project.rootDir}/.gradle/helm/cache"))


    @get:Internal
    final override val registryConfigFile: RegularFileProperty =
        project.objects.fileProperty()
            .convention(xdgConfigHome.file("helm/registry.json"))


    @get:Internal
    final override val repositoryCacheDir: DirectoryProperty =
        project.objects.directoryProperty()
            .convention(xdgCacheHome.dir("helm/repository"))


    @get:Internal
    final override val repositoryConfigFile: RegularFileProperty =
        project.objects.fileProperty()
            .convention(xdgConfigHome.file("helm/repositories.yaml"))


    protected fun execHelm(
        command: String, subcommand: String? = null, action: (HelmExecSpec.() -> Unit)? = null
    ): ExecResult =
        execProviderSupport.execHelm(command, subcommand, action)


    @get:Internal
    internal open val execProviderSupport: HelmExecProviderSupport
        get() = HelmExecProviderSupport(project, this, GlobalHelmOptionsApplier)
}
