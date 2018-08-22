package org.unbrokendome.gradle.plugins.helm.model

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.Reader


internal interface ChartDescriptor {
    val name: String?
    val version: String?
}


private class DefaultChartDescriptor(
        private val map: Map<String, Any?>)
    : ChartDescriptor {

    override val name
        get() = map["name"] as String?

    override val version
        get() = map["version"] as String?
}


private object EmptyChartDescriptor : ChartDescriptor {

    override val name: String?
        get() = null

    override val version: String?
        get() = null
}


internal object ChartDescriptorYaml {

    private val yaml = Yaml()


    fun loading(from: Provider<RegularFile>): Provider<ChartDescriptor> =
            from.map(this::load)


    fun load(file: RegularFile): ChartDescriptor =
            load(file.asFile)


    fun load(file: File): ChartDescriptor =
        file.takeIf { it.exists() }
                ?.reader()?.use(this::load)
                ?: EmptyChartDescriptor


    @Suppress("UNCHECKED_CAST")
    fun load(reader: Reader): ChartDescriptor =
            (yaml.load(reader) as? Map<String, Any?>)
                    ?.let(::DefaultChartDescriptor)
                    ?: EmptyChartDescriptor
}
