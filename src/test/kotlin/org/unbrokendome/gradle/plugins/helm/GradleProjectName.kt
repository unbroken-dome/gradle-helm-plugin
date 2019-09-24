package org.unbrokendome.gradle.plugins.helm


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class GradleProjectName(
    val value: String
)
