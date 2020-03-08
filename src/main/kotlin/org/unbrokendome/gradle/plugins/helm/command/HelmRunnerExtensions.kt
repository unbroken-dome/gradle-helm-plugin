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
 * @param fileValues a [Provider] of a [Map] containing values to be read from the contents of a file.
 * @param valueFiles a [FileCollection] of YAML files containing additional values to pass to the command.
 */
fun HelmExecSpec.valuesOptions(
    values: Provider<Map<String, Any>>,
    fileValues: Provider<Map<String, Any>>,
    valueFiles: FileCollection
) {
    val (stringValues, otherValues) = values.get().toList()
        .partition { it.second is String }
        .toList()
        .map { items ->
            items.joinToString(separator = ",") { (key, value) -> "$key=$value" }
        }
    if (stringValues.isNotEmpty()) {
        option("--set-string", stringValues)
    }
    if (otherValues.isNotEmpty()) {
        option("--set", otherValues)
    }

    option("--set-file",
        fileValues.map { m ->
            m.entries.joinToString(separator = ",") { (key, value) -> "$key=$value" }
        }
    )

    if (!valueFiles.isEmpty) {
        option("--values", valueFiles.joinToString(",") { it.absolutePath })
    }
}
