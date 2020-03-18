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


/**
 * Returns a single action that is equivalent to executing all the given actions in order of iteration.
 *
 * @receiver a [Collection] of [Action]s
 * @return a single [Action] that executes all given actions in order, or `null` if the collection is empty
 */
internal fun <T> Collection<Action<in T>>.combine(): Action<T>? {
    if (isEmpty()) return null

    return Action {
        for (action in this) {
            action.execute(it)
        }
    }
}
