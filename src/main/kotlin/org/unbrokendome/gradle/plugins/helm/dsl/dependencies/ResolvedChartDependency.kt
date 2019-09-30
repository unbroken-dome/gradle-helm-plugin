package org.unbrokendome.gradle.plugins.helm.dsl.dependencies

import java.io.File


internal data class ResolvedChartDependency(
    /** Resolved version. */
    val version: String,
    /** Path to the resolved chart artifact. */
    val file: File
)
