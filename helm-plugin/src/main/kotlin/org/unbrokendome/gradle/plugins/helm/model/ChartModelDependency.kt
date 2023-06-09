package com.citi.gradle.plugins.helm.model


/**
 * Represents a dependency inside the chart requirements.
 */
internal interface ChartModelDependency {

    val name: String
    val version: String?
    val repository: String?
    val alias: String?

    /**
     * Returns a new instance of [ChartModelDependency], where all attributes are equal to the current instance,
     * except that [repository] and [version] have been replaced with the given values.
     *
     * @param repository the new `repository` value
     * @param version the new `version` value
     * @returns the new [ChartModelDependency] instance
     */
    fun withRepositoryAndVersion(repository: String, version: String): ChartModelDependency


    companion object {

        fun fromMap(map: Map<String, Any?>): ChartModelDependency =
            DefaultChartModelDependency(
                name = (map["name"] as? String?).orEmpty(),
                version = map["version"] as String?,
                repository = map["repository"] as String?,
                alias = map["alias"] as String?
            )
    }
}


private data class DefaultChartModelDependency(
    override val name: String,
    override val version: String?,
    override val repository: String?,
    override val alias: String?
) : ChartModelDependency {

    override fun withRepositoryAndVersion(repository: String, version: String): ChartModelDependency =
        copy(repository = repository, version = version)
}
