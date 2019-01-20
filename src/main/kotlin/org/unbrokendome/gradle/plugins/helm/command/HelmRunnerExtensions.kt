package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider


/**
 * Adds common options for commands that take values (`install`, `upgrade`, `lint`).
 *
 * This will add `--set`, `--set-string` and `--values` based on the supplied properties.
 *
 * @param values a [Provider] of a [Map] containing directly specified values.
 *        Entries with [String] values will be added using `--set-string`; other types of values will be added
 *        using `--set`.
 * @param valueFiles a [FileCollection] of YAML files containing additional values to pass to the command.
 */
fun HelmExecSpec.valuesOptions(
        values: Provider<Map<String, Any>>,
        valueFiles: FileCollection) {

    val (stringValues, otherValues) = values.get().toList()
            .partition { it.second is String }
            .toList()
            .map { items ->
                items.joinToString(separator = ",") { "${it.first}=${it.second}" }
            }
    if (stringValues.isNotEmpty()) {
        option("--set-string", stringValues)
    }
    if (otherValues.isNotEmpty()) {
        option("--set", otherValues)
    }

    if (!valueFiles.isEmpty) {
        option("--values", valueFiles.joinToString(",") { it.absolutePath })
    }
}
