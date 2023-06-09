@file:JvmName("HelmPublishPluginExtensionAccessors")
package org.gradle.kotlin.dsl

import org.gradle.api.provider.Property
import com.citi.gradle.plugins.helm.dsl.HelmChart
import com.citi.gradle.plugins.helm.publishing.dsl.publishConvention


/**
 * Indicates whether tasks for publishing this chart should be created automatically.
 *
 * Defaults to `true`.
 */
val HelmChart.publish: Property<Boolean>
    get() = publishConvention.publish
