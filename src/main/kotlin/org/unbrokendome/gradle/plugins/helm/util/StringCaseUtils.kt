package org.unbrokendome.gradle.plugins.helm.util


/**
 * Split a string into words. All characters that are not letters or digits are considered separators.
 *
 * Upper-case letters also start a new word (camel case). The casing will not be changed in the output.
 *
 * If the input string is empty, the returned list will be empty. If the input string does not contain any word
 * separators, the returned list will contain one element that is identical to the input string.
 *
 * @receiver the string to split into words
 * @return a list of words in the string
 */
internal fun String.splitIntoWords(): List<String> {

    val words = mutableListOf<String>()
    val builder = StringBuilder()

    this.forEach { ch ->
        if (ch.isUpperCase()) {
            if (builder.isNotEmpty()) {
                words.add(builder.toString())
                builder.setLength(0)
            }
            builder.append(ch.toLowerCase())

        } else if (!ch.isLetterOrDigit()) {
            if (builder.isNotEmpty()) {
                words.add(builder.toString())
                builder.setLength(0)
            }

        } else {
            builder.append(ch)
        }
    }

    if (builder.isNotEmpty()) {
        words.add(builder.toString())
    }

    return words
}


/**
 * Splits a string into words, capitalizes each word and concatenates them into a single string (camel case).
 *
 * @receiver the string to modify
 * @param capitalizeFirst whether to capitalize the first word
 * @return the resulting string
 */
internal fun String.capitalizeWords(capitalizeFirst: Boolean = true): String =
        buildString {
            splitIntoWords().iterator()
                    .also {
                        if (!capitalizeFirst && it.hasNext()) {
                            append(it.next())
                        }
                    }
                    .forEachRemaining { word ->
                        append(word.capitalize())
                    }
        }
