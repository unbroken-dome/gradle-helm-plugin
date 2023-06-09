package com.citi.gradle.plugins.helm.util

import org.gradle.api.file.ContentFilterable
import java.io.Reader


/**
 * A [java.io.FilterReader] that modifies a given YAML file by overriding specific values.
 *
 * Overridden values must be provided by setting the `overrides` property.
 *
 * Any values that are already present in the source and have corresponding entries in [overrides] will be
 * overridden in-place with the new values. Entries in [overrides] that do not appear in the original source
 * will be appended at the end.
 */
@Suppress("MemberVisibilityCanBePrivate")
internal class YamlOverrideFilterReader(input: Reader) : AbstractYamlTransformingReader(input) {

    /*
     * Note: Properties of this class must be `var` because they are injected by Gradle's
     * ContentFilterable.filter method.
     */
    var overrides: Map<String, Any> = emptyMap()

    override fun transformScalar(path: YamlPath, value: String): String? =
        overrides[path.toString()]?.toString()
}


internal fun ContentFilterable.filterYaml(overrides: Map<String, Any>) =
    filter(
        mapOf("overrides" to overrides),
        YamlOverrideFilterReader::class.java
    )


internal fun ContentFilterable.filterYaml(vararg overrides: Pair<String, Any>) =
    filterYaml(mapOf(*overrides))
