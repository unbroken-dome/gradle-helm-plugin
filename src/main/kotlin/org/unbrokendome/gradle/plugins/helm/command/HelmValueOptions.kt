package org.unbrokendome.gradle.plugins.helm.command

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.resources.TextResource
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable


interface HelmValueOptions {

    /**
     * Values to be passed directly.
     *
     * Entries in the map will be sent to the CLI using either the `--set-string` option (for strings) or the
     * `--set` option (for all other types).
     */
    val values: MapProperty<String, Any>


    /**
     * Values read from the contents of files.
     *
     * Corresponds to the `--set-file` CLI option.
     *
     * The values of the map can be of any type that is accepted by [Project.file]. Additionally, when adding a
     * [Provider] that represents an output file of another task, the consuming task will automatically have a task
     * dependency on the producing task.
     *
     * Not to be confused with [valueFiles], which contains a collection of YAML files that supply multiple values.
     */
    val fileValues: MapProperty<String, Any>


    /**
     * A collection of YAML files containing values.
     *
     * Corresponds to the `--values` CLI option.
     *
     * Not to be confused with [fileValues], which contains entries whose values are the contents of files.
     */
    val valueFiles: ConfigurableFileCollection
}


internal object HelmValueOptionsApplier : HelmOptionsApplier {

    private val logger = LoggerFactory.getLogger(javaClass)


    override fun apply(spec: HelmExecSpec, options: HelmOptions) {
        if (options is HelmValueOptions) {

            logger.debug("Applying HelmValueOptions: {}", options)

            with(spec) {

                val (stringValues, otherValues) = options.values.getOrElse(emptyMap()).toList()
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
                    options.fileValues.map<String> { m ->

                        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                        m.takeIf { it.isNotEmpty() }
                            ?.entries?.joinToString(separator = ",") { (key, value) ->

                            val valueRepresentation = when (val resolvedValue = resolveValue(value)) {
                                is FileCollection -> resolvedValue.singleFile
                                is TextResource -> resolvedValue.asFile()
                                else -> resolvedValue
                            }

                            "$key=$valueRepresentation"

                        }
                    }
                )

                options.valueFiles.takeUnless { it.isEmpty }
                    ?.let { valueFiles ->
                        option("--values", valueFiles.joinToString(",") { it.absolutePath })
                    }
            }
        }
    }


    private fun resolveValue(value: Any?): Any? =
        when (value) {
            is Provider<*> -> resolveValue(value.orNull)
            is Callable<*> -> resolveValue(value.call())
            is Closure<*> -> resolveValue(value.call())
            else -> value
        }
}
