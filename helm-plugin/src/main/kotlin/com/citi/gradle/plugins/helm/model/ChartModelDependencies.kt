package com.citi.gradle.plugins.helm.model


/**
 * Provides access to the chart's declared dependencies.
 */
internal interface ChartModelDependencies {

    /**
     * The declared dependencies of the chart.
     */
    val dependencies: List<ChartModelDependency>

    companion object {

        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): ChartModelDependencies {
            val dependencies = (map["dependencies"] as? List<Map<String, Any?>>)
                ?.map { ChartModelDependency.fromMap(it) }
                ?.takeUnless { it.isEmpty() }
            return dependencies?.let { DefaultChartModelDependencies(it) } ?: Empty
        }


        val empty: ChartModelDependencies
            get() = Empty


        private object Empty : ChartModelDependencies {
            override val dependencies: List<ChartModelDependency>
                get() = emptyList()
        }
    }
}


internal fun ChartModelDependencies.map(mapper: (ChartModelDependency) -> ChartModelDependency): ChartModelDependencies =
    DefaultChartModelDependencies(dependencies.map(mapper))


private data class DefaultChartModelDependencies(
    override val dependencies: List<ChartModelDependency>
) : ChartModelDependencies
