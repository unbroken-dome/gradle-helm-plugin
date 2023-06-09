package com.citi.gradle.plugins.helm.command

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider


interface HelmValueOptions : HelmOptions {

    val values: Provider<Map<String, Any>>

    val fileValues: Provider<Map<String, Any>>

    val valueFiles: FileCollection
}


interface ConfigurableHelmValueOptions : HelmValueOptions, ConfigurableHelmOptions {

    /**
     * Values to be passed directly.
     *
     * Entries in the map will be sent to the CLI using either the `--set-string` option (for strings) or the
     * `--set` option (for all other types).
     */
    override val values: MapProperty<String, Any>


    /**
     * Values read from the contents of files.
     *
     * Corresponds to the `--set-file` CLI option.
     *
     * The values of the map can be of any type that is accepted by [Project.file]. Additionally, when adding a
     * [Provider] that represents an output file of another task, the consuming task will automatically have a task
     * dependency on the producing task.
     *
     * Not to be confused with [valueFiles], which contains a collection of YAML files that supply multiple values.
     */
    override val fileValues: MapProperty<String, Any>


    /**
     * A collection of YAML files containing values.
     *
     * Corresponds to the `--values` CLI option.
     *
     * Not to be confused with [fileValues], which contains entries whose values are the contents of files.
     */
    override val valueFiles: ConfigurableFileCollection
}
