@file:JvmName("HelmPluginExtensionAccessors")
@file:Suppress("unused")

package org.gradle.kotlin.dsl

import com.citi.gradle.plugins.helm.HELM_DEPENDENCIES_EXTENSION_NAME
import com.citi.gradle.plugins.helm.HELM_FILTERING_EXTENSION_NAME
import com.citi.gradle.plugins.helm.HELM_LINT_EXTENSION_NAME
import com.citi.gradle.plugins.helm.dsl.Filtering
import com.citi.gradle.plugins.helm.dsl.HelmChart
import com.citi.gradle.plugins.helm.dsl.Linting
import com.citi.gradle.plugins.helm.dsl.dependencies.ChartDependencyHandler
import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware
import org.unbrokendome.gradle.pluginutils.requiredExtension


/**
 * Gets the chart's [Linting] extension.
 */
val HelmChart.lint: Linting
    get() = requiredExtension(HELM_LINT_EXTENSION_NAME)


/**
 * Configures the chart linting.
 */
fun HelmChart.lint(configure: Action<Linting>) =
    (this as ExtensionAware).extensions.configure(HELM_LINT_EXTENSION_NAME, configure)


/**
 * Gets the chart's [Filtering] extension.
 */
val HelmChart.filtering: Filtering
    get() = requiredExtension(HELM_FILTERING_EXTENSION_NAME)


/**
 * Configures the chart filtering.
 */
fun HelmChart.filtering(configure: Action<Filtering>) =
    (this as ExtensionAware).extensions.configure(HELM_FILTERING_EXTENSION_NAME, configure)


/**
 * Gets the chart's `dependencies` extension.
 */
val HelmChart.dependencies: ChartDependencyHandler
    get() = requiredExtension(HELM_DEPENDENCIES_EXTENSION_NAME)


/**
 * Configures the chart dependencies.
 */
fun HelmChart.dependencies(configure: Action<ChartDependencyHandler>) =
    (this as ExtensionAware).extensions.configure(HELM_DEPENDENCIES_EXTENSION_NAME, configure)
