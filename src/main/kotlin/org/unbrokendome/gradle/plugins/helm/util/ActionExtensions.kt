package org.unbrokendome.gradle.plugins.helm.util

import org.gradle.api.Action


/**
 * Returns a new action that executes this action and then another action.
 *
 * @receiver the first action to execute
 * @param other the other action to execute
 * @return the combined action
 */
fun <T> Action<T>.andThen(other: Action<T>): Action<T> =
    Action {
        this.execute(it)
        other.execute(it)
    }


/**
 * Returns a new action that executes this action and then another action.
 *
 * @receiver the first action to execute
 * @param other the other action to execute
 * @return the combined action
 */
fun <T> Action<T>.andThen(other: T.() -> Unit): Action<T> =
    andThen(Action(other))
