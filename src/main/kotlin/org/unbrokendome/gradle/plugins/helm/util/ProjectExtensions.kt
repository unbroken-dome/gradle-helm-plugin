package org.unbrokendome.gradle.plugins.helm.util

import org.gradle.api.Project
import org.gradle.api.provider.Provider


/**
 * A [Provider] that returns the project's version.
 */
val Project.versionProvider : Provider<String>
    get() = provider { version.toString() }


fun <T : Any> Project.coalesceProvider(vararg providers: Provider<out T>): Provider<T> = provider {
    providers.asSequence()
            .mapNotNull { it.orNull }
            .firstOrNull()
}
