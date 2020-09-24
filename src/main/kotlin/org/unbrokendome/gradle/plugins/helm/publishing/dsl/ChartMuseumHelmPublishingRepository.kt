package org.unbrokendome.gradle.plugins.helm.publishing.dsl

import org.gradle.api.model.ObjectFactory
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.SerializableCredentials
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.toSerializable
import org.unbrokendome.gradle.plugins.helm.publishing.publishers.AbstractHttpHelmChartPublisher
import org.unbrokendome.gradle.plugins.helm.publishing.publishers.HelmChartPublisher
import org.unbrokendome.gradle.plugins.helm.publishing.publishers.PublisherParams
import java.net.URI
import javax.inject.Inject


interface ChartMuseumHelmPublishingRepository : HelmPublishingRepository


private abstract class DefaultChartMuseumHelmPublishingRepository
@Inject constructor(
    name: String,
    objects: ObjectFactory
) : AbstractHelmPublishingRepository(objects, name), ChartMuseumHelmPublishingRepository {

    override val publisherParams: PublisherParams
        get() = ChartMuseumPublisherParams(
            url = url.get(), credentials = configuredCredentials.orNull?.toSerializable()
        )


    private class ChartMuseumPublisherParams(
        private val url: URI,
        private val credentials: SerializableCredentials?
    ) : PublisherParams {

        override fun createPublisher(): HelmChartPublisher =
            ChartMuseumPublisher(url, credentials)
    }


    private class ChartMuseumPublisher(
        url: URI,
        credentials: SerializableCredentials?
    ) : AbstractHttpHelmChartPublisher(url, credentials) {

        override val uploadMethod: String
            get() = "POST"

        override fun uploadPath(chartName: String, chartVersion: String): String =
            "/api/charts"
    }
}


internal fun ObjectFactory.newChartMuseumHelmPublishingRepository(name: String): ChartMuseumHelmPublishingRepository =
    newInstance(DefaultChartMuseumHelmPublishingRepository::class.java, name)
