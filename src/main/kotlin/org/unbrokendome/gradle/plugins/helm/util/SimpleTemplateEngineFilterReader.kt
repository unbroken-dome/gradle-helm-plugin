package org.unbrokendome.gradle.plugins.helm.util

import groovy.text.SimpleTemplateEngine
import org.gradle.api.file.ContentFilterable
import java.io.Reader
import java.io.StringReader
import java.io.StringWriter


/**
 * A transforming [Reader] that will use a Groovy [SimpleTemplateEngine].
 *
 * Similar to what Gradle's built-in [ContentFilterable.expand] does, but that one does not expose the option
 * to expose the `escapeBackslashes` property, so we need to create our own wrapper.
 */
internal class SimpleTemplateEngineFilterReader(
    input: Reader
) : DelegateReader(input) {

    var properties: Map<String, *> = emptyMap<String, Any?>()
    var escapeBackslash: Boolean = false


    override val delegate: Reader by lazy(LazyThreadSafetyMode.NONE) {

        val template = `in`.use { input ->
            val engine = SimpleTemplateEngine()
            engine.isEscapeBackslash = escapeBackslash
            engine.createTemplate(input)
        }

        val writer = StringWriter()
        template.make(properties).writeTo(writer)

        StringReader(writer.toString())
    }
}


internal fun ContentFilterable.expand(properties: Map<String, *>, escapeBackslash: Boolean) =
    filter(
        mapOf(
            "properties" to properties,
            "escapeBackslash" to escapeBackslash
        ),
        SimpleTemplateEngineFilterReader::class.java
    )
