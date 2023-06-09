package com.citi.gradle.plugins.helm.command.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.unbrokendome.gradle.pluginutils.listProperty
import org.unbrokendome.gradle.pluginutils.property


/**
 * Renders chart templates locally.
 */
open class HelmTemplate : AbstractHelmInstallationCommandTask() {

    @get:[Input Optional]
    override val releaseName: Property<String>
        get() = super.releaseName


    /**
     * Kubernetes API versions used for `Capabilities.APIVersions`.
     *
     * Corresponds to the `--api-versions` CLI option.
     */
    @get:Input
    val apiVersions: ListProperty<String> =
        project.objects.listProperty()

    /**
     * If `true`, re-use the given release name, even if that name is already used.
     *
     * Corresponds to the `--replace` CLI option.
     */
    @get:Internal
    val replace: Property<Boolean> =
        project.objects.property()


    /**
     * If `true`, set `Release.IsUpgrade` instead of `Release.IsInstall`.
     *
     * Corresponds to the `--is-upgrade` CLI option.
     */
    @get:[Input Optional JvmName("getIsUpgrade")]
    val isUpgrade: Property<Boolean> =
        project.objects.property()


    /**
     * If not empty, only output manifests rendered from the given templates.
     *
     * Corresponds to the `--show-only` CLI option.
     */
    @get:Input
    val showOnly: ListProperty<String> =
        project.objects.listProperty()


    /**
     * If `true`, validate your manifests against the Kubernetes cluster you are currently pointing at.
     * This is the same validation performed on an install.
     *
     * Corresponds to the `--validate` CLI option.
     */
    @get:Internal
    val validate: Property<Boolean> =
        project.objects.property()


    /**
     * If `true`, use the release name in the output path.
     *
     * Corresponds to the `--release-name` CLI option.
     */
    @get:[Input Optional]
    val useReleaseNameInOutputPath: Property<Boolean> =
        project.objects.property()


    /**
     * Directory into which the template output files will be written.
     *
     * Corresponds to the `--output-dir` CLI option.
     */
    @get:OutputDirectory
    val outputDir: DirectoryProperty =
        project.objects.directoryProperty()


    @TaskAction
    fun renderTemplate() {

        project.delete(outputDir)

        execHelm("template") {
            args(releaseName)
            args(chart)
            option("--output-dir", outputDir.asFile)
            option("--version", version)
            option("--replace", replace)
            option("--api-versions", apiVersions.map { it.joinToString(",") })
            option("--show-only", showOnly.flatMap { showOnly ->
                if (showOnly.isNotEmpty()) {
                    project.provider { showOnly.joinToString(",") }
                } else {
                    project.provider<String> { null }
                }
            })
            flag("--is-upgrade", isUpgrade)
            flag("--release-name", useReleaseNameInOutputPath)
            flag("--validate", validate)
        }
    }
}
