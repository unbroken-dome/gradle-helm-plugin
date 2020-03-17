package org.unbrokendome.gradle.plugins.helm.util

import org.gradle.api.Action


/**
 * Returns a new action that executes this action and then another action.
 *
 * @receiver the first action to execute, `null` is interpreted as an empty action
 * @param other the other action to execute, `null` is interpreted as an empty action
 * @return the combined action, or `null` if both input actions are `null`
 */
internal fun <T> Action<T>?.andThen(other: Action<T>?): Action<T>? =
    when {
        this == null -> other
        other == null -> this
        else -> Action {
            this.execute(it)
            other.execute(it)
        }
    }
