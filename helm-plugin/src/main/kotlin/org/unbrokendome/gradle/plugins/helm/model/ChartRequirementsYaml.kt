package com.citi.gradle.plugins.helm.model

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.Reader


/**
 * Reads chart dependencies from a _requirements.yaml_ file.
 */
internal object ChartRequirementsYaml {

    fun loading(from: Provider<RegularFile>): Provider<ChartModelDependencies> =
        from.map { load(it) }


    fun load(file: RegularFile): ChartModelDependencies =
        load(file.asFile)


    /**
     * Reads a _requirements.yaml_ file into a [ChartModelDependencies] object.
     *
     * @param reader the [Reader] to read the YAML file
     * @return a new [ChartModelDependencies] instance
     */
    @Suppress("UNCHECKED_CAST")
    fun load(reader: Reader): ChartModelDependencies {
        val map = Yaml().load(reader) as Map<String, Any?>
        return ChartModelDependencies.fromMap(map)
    }

    /**
     * Reads a _requirements.yaml_ file into a [ChartModelDependencies] object.
     *
     * @param file the path to the _requirements.yaml_ file
     * @return a new [ChartModelDependencies] instance
     */
    fun load(file: File): ChartModelDependencies =
        file.takeIf { it.exists() }
            ?.reader()?.use { load(it) }
            ?: ChartModelDependencies.empty
}
