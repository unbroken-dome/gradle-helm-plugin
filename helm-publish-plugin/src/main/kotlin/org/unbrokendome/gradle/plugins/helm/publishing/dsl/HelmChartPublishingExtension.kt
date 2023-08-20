package org.unbrokendome.gradle.plugins.helm.publishing.dsl

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.unbrokendome.gradle.pluginutils.property
import javax.inject.Inject


/**
 * Extension that adds publishing-related properties to each chart.
 */
interface HelmChartPublishingExtension {

    /**
     * Indicates whether tasks for publishing this chart should be created automatically.
     *
     * Defaults to `true`.
     */
    val autoCreateTasks: Property<Boolean>
}


private open class DefaultHelmChartPublishingExtension
@Inject constructor(objectFactory: ObjectFactory) : HelmChartPublishingExtension {

    override val autoCreateTasks: Property<Boolean> =
        objectFactory.property<Boolean>()
            .convention(true)
}


/**
 * Creates a new [HelmChartPublishingExtension] using the given [ObjectFactory].
 *
 * @receiver the [ObjectFactory] used to instantiate the convention object
 * @return the [HelmChartPublishingExtension]
 */
internal fun ObjectFactory.createHelmChartPublishingExtension(): HelmChartPublishingExtension =
    newInstance(DefaultHelmChartPublishingExtension::class.java)
