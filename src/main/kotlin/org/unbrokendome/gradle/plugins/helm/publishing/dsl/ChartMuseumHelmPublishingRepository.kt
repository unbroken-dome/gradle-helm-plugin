package org.unbrokendome.gradle.plugins.helm.publishing.dsl

import org.gradle.api.credentials.Credentials
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.unbrokendome.gradle.plugins.helm.publishing.publishers.AbstractHttpHelmChartPublisher
import org.unbrokendome.gradle.plugins.helm.publishing.publishers.HelmChartPublisher
import java.net.URI
import javax.inject.Inject


interface ChartMuseumHelmPublishingRepository : HelmPublishingRepository


private open class DefaultChartMuseumHelmPublishingRepository
@Inject constructor(
    name: String,
    objects: ObjectFactory
) : AbstractHelmPublishingRepository(objects, name), ChartMuseumHelmPublishingRepository {

    override val publisher: HelmChartPublisher
        get() = Publisher(url.get(), configuredCredentials)


    private class Publisher(url: URI, credentials: Provider<Credentials>) :
        AbstractHttpHelmChartPublisher(url, credentials) {

        override val uploadMethod: String
            get() = "POST"

        override fun uploadPath(chartName: String, chartVersion: String): String =
            "/api/charts"
    }
}


internal fun ObjectFactory.newChartMuseumHelmPublishingRepository(name: String): ChartMuseumHelmPublishingRepository =
    newInstance(DefaultChartMuseumHelmPublishingRepository::class.java, name)