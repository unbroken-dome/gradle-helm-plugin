package org.unbrokendome.gradle.plugins.helm.publishing.dsl

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.plugins.helm.util.property
import org.unbrokendome.gradle.plugins.helm.util.requiredConvention
import javax.inject.Inject


/**
 * Convention that adds publishing-related properties to each chart.
 */
interface HelmChartPublishConvention {

    /**
     * Indicates whether tasks for publishing this chart should be created automatically.
     *
     * Defaults to `true`.
     */
    val publish: Property<Boolean>
}


private open class DefaultHelmChartPublishConvention
@Inject constructor(objectFactory: ObjectFactory) : HelmChartPublishConvention {

    override val publish: Property<Boolean> =
        objectFactory.property<Boolean>()
            .convention(true)
}


/**
 * Creates a new [HelmChartPublishConvention] using the given [ObjectFactory].
 *
 * @receiver the [ObjectFactory] used to instantiate the convention object
 * @return the [HelmChartPublishConvention]
 */
internal fun ObjectFactory.createHelmChartPublishConvention(): HelmChartPublishConvention =
    newInstance(DefaultHelmChartPublishConvention::class.java)


/**
 * Gets the [HelmChartPublishConvention] object for the given chart.
 */
internal val HelmChart.publishConvention: HelmChartPublishConvention
    get() = requiredConvention()
