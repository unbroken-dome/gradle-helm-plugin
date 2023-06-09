package com.citi.gradle.plugins.helm.model

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.Reader


internal interface ChartDescriptor : ChartModelDependencies {

    /** The API version of the chart. */
    val apiVersion: String

    /** The name of the chart. */
    val name: String?

    /** The version of the chart. */
    val version: String?

    /**
     * The type of chart.
     */
    val type: String?

    companion object {
        fun fromMap(map: Map<String, Any?>): ChartDescriptor {
            val apiVersion = (map["apiVersion"] as String?) ?: ChartApiVersion.DEFAULT

            return DefaultChartDescriptor(
                apiVersion = apiVersion,
                name = map["name"] as String?,
                version = map["version"]?.toString(),
                dependencies = if (apiVersion != ChartApiVersion.V1) {
                    ChartModelDependencies.fromMap(map).dependencies
                } else emptyList(),
                type = map["type"]?.toString()
            )
        }
    }
}


private data class DefaultChartDescriptor(
    override val apiVersion: String,
    override val name: String?,
    override val version: String?,
    override val dependencies: List<ChartModelDependency> = emptyList(),
    override val type: String?
) : ChartDescriptor


private object EmptyChartDescriptor : ChartDescriptor {

    override val apiVersion: String
        get() = ChartApiVersion.DEFAULT

    override val name: String?
        get() = null

    override val version: String?
        get() = null

    override val dependencies: List<ChartModelDependency>
        get() = emptyList()

    override val type: String?
        get() = null
}


internal object ChartDescriptorYaml {

    fun loading(from: Provider<RegularFile>): Provider<ChartDescriptor> =
        from.map { load(it) }


    fun load(file: RegularFile): ChartDescriptor =
        load(file.asFile)


    fun load(file: File): ChartDescriptor =
        file.takeIf { it.exists() }
            ?.reader()?.use { load(it) }
            ?: EmptyChartDescriptor


    @Suppress("UNCHECKED_CAST")
    fun load(reader: Reader): ChartDescriptor =
        (Yaml().load(reader) as? Map<String, Any?>)
            ?.let { map -> ChartDescriptor.fromMap(map) }
            ?: EmptyChartDescriptor
}
