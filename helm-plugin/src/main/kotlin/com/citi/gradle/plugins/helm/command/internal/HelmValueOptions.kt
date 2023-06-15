package com.citi.gradle.plugins.helm.command.internal

import groovy.lang.Closure
import java.util.concurrent.Callable
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.resources.TextResource
import org.slf4j.LoggerFactory
import com.citi.gradle.plugins.helm.command.ConfigurableHelmValueOptions
import com.citi.gradle.plugins.helm.command.HelmExecSpec
import com.citi.gradle.plugins.helm.command.HelmOptions
import com.citi.gradle.plugins.helm.command.HelmValueOptions
import org.unbrokendome.gradle.pluginutils.mapProperty


data class HelmValueOptionsHolder(
    override val values: MapProperty<String, Any>,
    override val fileValues: MapProperty<String, Any>,
    override val valueFiles: ConfigurableFileCollection
) : ConfigurableHelmValueOptions {

    constructor(objects: ObjectFactory) : this(
        values = objects.mapProperty(),
        fileValues = objects.mapProperty(),
        valueFiles = objects.fileCollection()
    )
}


object HelmValueOptionsApplier : HelmOptionsApplier {

    private val logger = LoggerFactory.getLogger(javaClass)


    override fun apply(spec: HelmExecSpec, options: HelmOptions) {
        if (options is HelmValueOptions) {

            logger.debug("Applying options: {}", options)

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

                buildFileValuesArg(options).takeIf { it.isNotBlank() }?.let { arg ->
                    option("--set-file", arg)
                }

                options.valueFiles.takeUnless { it.isEmpty }
                    ?.let { valueFiles ->
                        option("--values", valueFiles.joinToString(",") { it.absolutePath })
                    }
            }
        }
    }

    private fun buildFileValuesArg(options: HelmValueOptions): String =
        options.fileValues.getOrElse(emptyMap())
            .entries
            .joinToString(separator = ",") { (key, value) ->
                val valueRepresentation = when (val resolvedValue = resolveValue(value)) {
                    is FileCollection -> resolvedValue.singleFile
                    is TextResource -> resolvedValue.asFile()
                    else -> resolvedValue
                }

                "$key=$valueRepresentation"
            }

    private fun resolveValue(value: Any?): Any? =
        when (value) {
            is Provider<*> -> resolveValue(value.orNull)
            is Callable<*> -> resolveValue(value.call())
            is Closure<*> -> resolveValue(value.call())
            else -> value
        }
}


fun ConfigurableHelmValueOptions.mergeValues(toMerge: HelmValueOptions) = apply {
    values.putAll(toMerge.values)
    fileValues.putAll(toMerge.fileValues)
    valueFiles.from(toMerge.valueFiles)
}
