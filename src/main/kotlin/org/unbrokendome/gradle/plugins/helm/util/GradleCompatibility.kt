package org.unbrokendome.gradle.plugins.helm.util


/**
 * Indicates that a feature is required for supporting older Gradle versions.
 *
 * This annotation is intended to make those code parts easier to spot once we require a minimum Gradle version
 * that makes them unnecessary.
 */
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
internal annotation class GradleCompatibility(
    val value: String,
    val reason: String = ""
)
