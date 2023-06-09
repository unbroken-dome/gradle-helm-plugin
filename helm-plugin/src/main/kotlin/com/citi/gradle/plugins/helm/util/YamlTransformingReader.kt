package com.citi.gradle.plugins.helm.util

import org.unbrokendome.gradle.pluginutils.io.DelegateReader
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.Mark
import org.yaml.snakeyaml.error.YAMLException
import org.yaml.snakeyaml.events.*
import java.io.Reader
import java.io.StringReader
import java.io.StringWriter
import java.io.Writer


internal class YamlPath(val elements: List<Element>) {

    @Suppress("MemberVisibilityCanBePrivate")
    sealed class Element {

        class MappingKey(val name: String) : Element() {
            override fun toString() = name
        }

        class SequenceIndex(val index: Int) : Element() {
            override fun toString() = "[$index]"
        }
    }

    override fun toString(): String = buildString {
        for (element in elements) {
            if (element is Element.MappingKey && length > 0) {
                append('.')
            }
            append(element)
        }
    }
}


internal abstract class AbstractYamlTransformingReader(
    input: Reader
) : DelegateReader(input) {

    /**
     * Transform a scalar value.
     *
     * @param path the [YamlPath] of the scalar
     * @param value the _raw_ value of the scalar (i.e. possibly containing quotes, line breaks, style indicators etc.)
     * @return the new value, or `null` to remove this entry
     */
    protected open fun transformScalar(path: YamlPath, value: String): String? = value


    /**
     * Inject additional entries at the end of a YAML mapping.
     *
     * @param path the [YamlPath] of the current YAML mapping
     * @return a [Map] containing additional entries to inject
     */
    protected open fun addToMapping(path: YamlPath): Map<String, String> = emptyMap()


    override fun createDelegateReader(input: Reader): Reader {
        val events = Yaml().parse(input)
        var currentMark: Mark? = null
        var state: ParseState = Initial()
        val output = StringWriter()

        for (event in events) {
            if (currentMark != null && event.startMark.index > currentMark.index) {
                output.write(readFromBuffer(currentMark, event.startMark))
            }

            state = state.handleEvent(event, output) ?: break
            currentMark = event.endMark
        }

        return StringReader(output.toString())
    }


    private interface ParseState {

        /**
         * Handles a YAML parser event.
         *
         * @param event the YAML [Event] to handle
         * @param writer the output [Writer]
         * @return the next [ParseState], or `null` to stop
         */
        fun handleEvent(event: Event, writer: Writer): ParseState?
    }


    private abstract inner class ValueState(val path: List<YamlPath.Element>) : ParseState {

        protected fun handleValueEvent(event: Event, writer: Writer, next: ParseState?): ParseState? =
            when (event) {
                is ScalarEvent -> {
                    val value = readFromBuffer(event.startMark, event.endMark)
                    val transformed = transformScalar(YamlPath(path), value) ?: value
                    writer.write(transformed)
                    next
                }
                else -> {
                    writeEventChars(event, writer)
                    when (event) {
                        is MappingStartEvent -> MappingState(path, event, next)
                        is SequenceStartEvent -> SequenceItemState(path, 0, next)
                        else -> next
                    }
                }
            }
    }


    private inner class Initial : ValueState(emptyList()) {
        override fun handleEvent(event: Event, writer: Writer): ParseState? =
            when (event) {
                is StreamStartEvent, is DocumentStartEvent -> {
                    writeEventChars(event, writer)
                    this
                }
                is StreamEndEvent -> {
                    writeEventChars(event, writer)
                    null
                }
                else -> handleValueEvent(event, writer, null)
            }
    }


    private inner class SequenceItemState(
        path: List<YamlPath.Element>,
        private val index: Int,
        private val next: ParseState?
    ) : ValueState(path + YamlPath.Element.SequenceIndex(index)) {

        override fun handleEvent(event: Event, writer: Writer): ParseState? =
            if (event is SequenceEndEvent) {
                writeEventChars(event, writer)
                next
            } else handleValueEvent(event, writer, SequenceItemState(path.dropLast(1), index + 1, next))
    }


    private inner class MappingState(
        private val path: List<YamlPath.Element>,
        private val mappingStartEvent: MappingStartEvent,
        private val next: ParseState?
    ) : ParseState {
        override fun handleEvent(event: Event, writer: Writer): ParseState? =
            when (event) {
                is MappingEndEvent -> {

                    val extraEntries = addToMapping(YamlPath(path))
                    if (extraEntries.isNotEmpty()) {
                        if (mappingStartEvent.isFlow) {
                            extraEntries.forEach { (key, value) ->
                                writer.write(", $key: ${value.quoteValue()}")
                            }
                        } else {
                            val indent = " ".repeat(mappingStartEvent.startMark.column)
                            extraEntries.forEach { (key, value) ->

                                // replacement appendLine was added in 1.4,
                                // use legacy method for now to keep compatible with Gradle pre 6.8
                                @Suppress("DEPRECATION")
                                writer.appendln()
                                writer.write("${indent}$key: ${value.quoteValue()}")
                            }
                        }
                    }

                    writeEventChars(event, writer)
                    next
                }
                is ScalarEvent -> {
                    val value = readFromBuffer(event.startMark, event.endMark)
                    writer.write(value)
                    MappingValueState(path + YamlPath.Element.MappingKey(event.value), this)
                }
                else ->
                    throw YAMLException("Unexpected event: $event")
            }


        private fun String.quoteValue() =
            '"' + replace("\"", "\\\"") + '"'
    }


    private inner class MappingValueState(
        path: List<YamlPath.Element>,
        private val next: ParseState?
    ) : ValueState(path) {
        override fun handleEvent(event: Event, writer: Writer): ParseState? =
            handleValueEvent(event, writer, next)
    }
}


/**
 * Reads all characters between two marks.
 *
 * @param startMark a [Mark] indicating the start (inclusive)
 * @param endMark a [Mark] indicating the end (exclusive)
 * @return a string containing the characters between [startMark] and [endMark]
 */
private fun readFromBuffer(startMark: Mark, endMark: Mark): String {

    val length = endMark.index - startMark.index

    return when {
        length == 0 -> ""

        endMark.pointer >= length -> {
            // All the characters are present in the endMark buffer - just construct a string from the code points
            String(endMark.buffer, endMark.pointer - length, length)
        }

        else -> {
            // Start with the codepoints from startMark's buffer
            val startPartLength = startMark.buffer.size - startMark.pointer
            val startPart = String(startMark.buffer, startMark.pointer, startPartLength)

            val codePointsLeft = length - startPartLength
            if (endMark.pointer < codePointsLeft) {
                throw IllegalStateException("Buffer unavailable")
            }

            // Append codepoints from endMark's buffer
            val endPart = String(endMark.buffer, endMark.pointer - codePointsLeft, codePointsLeft)

            startPart + endPart
        }
    }
}


/**
 * Writes characters from a parser event to the output.
 *
 * Note: this does not use the event's `value` but rather the [Event.startMark] and [Event.endMark] information
 * to copy the characters in a raw fashion.
 *
 * @param event the YAML [Event]
 * @param writer the output [Writer]
 */
private fun writeEventChars(event: Event, writer: Writer) {
    if (event.endMark.index > event.startMark.index) {
        writer.write(readFromBuffer(event.startMark, event.endMark))
    }
}
