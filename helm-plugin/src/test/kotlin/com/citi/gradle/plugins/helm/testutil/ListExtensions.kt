package com.citi.gradle.plugins.helm.testutil


/**
 * Checks whether this list starts with a "prefix" of the given elements.
 *
 * @receiver the list
 * @param elements the elements to check as prefix
 * @return `true` if this list starts with [elements], otherwise `false`
 */
internal fun <T> List<T>.startsWith(elements: Iterable<T>) =
    elements.withIndex().all { (index, element) ->
        size > index && get(index) == element
    }
