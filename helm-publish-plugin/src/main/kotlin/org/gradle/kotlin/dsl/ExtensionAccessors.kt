@file:JvmName("HelmPublishPluginExtensionAccessors")
package org.gradle.kotlin.dsl

import org.gradle.api.provider.Property
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.plugins.helm.publishing.dsl.HelmChartPublishingExtension
import org.unbrokendome.gradle.pluginutils.requiredExtension


/**
 * Gets the [HelmChartPublishingExtension] object for the given chart.
 */
val HelmChart.publishing: HelmChartPublishingExtension
    get() = requiredExtension()


/**
 * Indicates whether tasks for publishing this chart should be created automatically.
 *
 * Defaults to `true`.
 */
@Deprecated(
    message = "Use publishing extension",
    replaceWith = ReplaceWith("publishing.enabled")
)
val HelmChart.publish: Property<Boolean>
    get() = publishing.enabled
