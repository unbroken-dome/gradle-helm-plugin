package org.unbrokendome.gradle.plugins.helm.model

import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.Reader


/**
 * Reads chart dependencies from a _requirements.yaml_ file.
 */
internal object ChartRequirementsYaml {

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
            ?.reader()?.use(this::load)
            ?: ChartModelDependencies.empty
}
