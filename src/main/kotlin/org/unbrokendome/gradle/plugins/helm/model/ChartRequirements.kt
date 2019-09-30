package org.unbrokendome.gradle.plugins.helm.model

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.Reader
import java.io.StringWriter
import java.io.Writer


/**
 * Models the contents of the _requirements.yaml_ file.
 */
internal interface ChartRequirements {

    val dependencies: List<Dependency>

    /**
     * Returns a new instance of [ChartRequirements], where each dependency has been transformed by
     * the given lambda.
     * @param transform a lambda that transforms the dependencies from the input object
     * @return the new [ChartRequirements] instance
     */
    fun withMappedDependencies(transform: (Dependency) -> Dependency): ChartRequirements

    /**
     * Returns the chart requirements as a [Map], which can be rendered directly into the _requirements.yaml_ file.
     */
    fun toMap(): Map<String, Any?>

    /**
     * Represents a dependency inside the chart requirements.
     */
    interface Dependency {
        val name: String
        val version: String?
        val repository: String?
        val alias: String?

        /**
         * Returns a new instance of [Dependency], where all attributes are equal to the current instance, except
         * that [repository] and [version] have been replaced with the given values.
         *
         * @param repository the new `repository` value
         * @param version the new `version` value
         * @returns the new [Dependency] instance
         */
        fun withRepositoryAndVersion(repository: String, version: String): Dependency

        /**
         * Returns this dependency as a [Map], which can be rendered directly into the _requirements.yaml_ file.
         */
        fun toMap(): Map<String, Any?>
    }
}


private class DefaultChartRequirements(
    private val map: Map<String, Any?>
) : ChartRequirements {


    @Suppress("UNCHECKED_CAST")
    override val dependencies: List<ChartRequirements.Dependency>
        get() =
            (map["dependencies"] as? List<Map<String, Any?>>)
                ?.map { DefaultDependency(it) }
                ?: emptyList()


    override fun withMappedDependencies(
        transform: (ChartRequirements.Dependency) -> ChartRequirements.Dependency
    ): ChartRequirements =
        DefaultChartRequirements(
            map + ("dependencies" to dependencies.map { transform(it).toMap() })
        )


    override fun toMap(): Map<String, Any?> =
        this.map


    private class DefaultDependency(
        private val map: Map<String, Any?>
    ) : ChartRequirements.Dependency {

        override val name: String
            get() = map["name"] as String


        override val version: String?
            get() = map["version"] as String?


        override val repository: String?
            get() = map["repository"] as String?


        override val alias: String?
            get() = map["alias"] as String?


        override fun withRepositoryAndVersion(repository: String, version: String): ChartRequirements.Dependency =
            DefaultDependency(map + mapOf("repository" to repository, "version" to version))


        override fun toMap(): Map<String, Any?> =
            this.map.toMap()
    }
}


/**
 * Reads and writes the _requirements.yaml_ file.
 */
internal object ChartRequirementsYaml {

    private val yaml = Yaml(DumperOptions().apply {
        this.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        this.isPrettyFlow = true
    })


    /**
     * Reads a _requirements.yaml_ file into a [ChartRequirements] object.
     *
     * @param reader the [Reader] to read the YAML file
     * @return a new [ChartRequirements] instance
     */
    @Suppress("UNCHECKED_CAST")
    fun load(reader: Reader): ChartRequirements {
        val map = yaml.load(reader) as Map<String, Any?>
        return DefaultChartRequirements(map)
    }


    private fun save(chartRequirements: ChartRequirements, writer: Writer) {
        yaml.dump(chartRequirements.toMap(), writer)
    }


    /**
     * Saves a [ChartRequirements] object in YAML format, and returns the YAML representation as a string.
     *
     * @param chartRequirements the [ChartRequirements] object
     * @return the YAML representation as a string
     */
    fun saveToString(chartRequirements: ChartRequirements) =
        StringWriter().use { writer ->
            save(chartRequirements, writer)
            writer.toString()
        }
}
