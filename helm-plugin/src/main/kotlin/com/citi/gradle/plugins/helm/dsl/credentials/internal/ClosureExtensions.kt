package com.citi.gradle.plugins.helm.dsl.credentials.internal

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.util.ConfigureUtil

/**
 * [ConfigureUtil] is deprecated in Gradle, and it will be removed from Gradle 9.
 *
 * To avoid [ClassNotFoundException] in calling class, let's extract the problematic method into special class and add new tests for Groovy language.
 *
 * Then we will be able to safely remove old methods.
 */
internal fun <T> Closure<*>.toAction(): Action<T> {
    return ConfigureUtil.configureUsing(this)
}