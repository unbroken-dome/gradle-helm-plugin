@file:Suppress("unused")

package org.gradle.kotlin.dsl

import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.unbrokendome.gradle.plugins.helm.HELM_DEPENDENCIES_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.HELM_FILTERING_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.HELM_LINT_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.dsl.Filtering
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.plugins.helm.dsl.Linting
import org.unbrokendome.gradle.plugins.helm.dsl.dependencies.ChartDependencyHandler
import org.unbrokendome.gradle.plugins.helm.publishing.dsl.publishConvention
import org.unbrokendome.gradle.plugins.helm.util.requiredExtension


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


/**
 * Indicates whether tasks for publishing this chart should be created automatically.
 *
 * Defaults to `true`.
 */
val HelmChart.publish: Property<Boolean>
    get() = publishConvention.publish
