package org.unbrokendome.gradle.plugins.helm.publishing.dsl

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.SerializableCredentials
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.toSerializable
import org.unbrokendome.gradle.plugins.helm.publishing.publishers.AbstractHttpHelmChartPublisher
import org.unbrokendome.gradle.plugins.helm.publishing.publishers.HelmChartPublisher
import org.unbrokendome.gradle.plugins.helm.publishing.publishers.PublisherParams
import java.net.URI
import javax.inject.Inject


interface CustomHelmPublishingRepository : HelmPublishingRepository {

    /**
     * The HTTP method used to upload the chart packages.
     *
     * Defaults to `POST`.
     */
    val uploadMethod: Property<String>

    /**
     * The path, relative to the base [url], with the API endpoint for publishing charts.
     *
     * May contain the following placeholders:
     *
     * - `{name}` will be replaced with the chart name
     * - `{version}` will be replaced with the chart version
     * - `{filename}` will be replaced with the filename of the packaged chart, i.e. `{name}-{version}.tgz`
     *
     * Defaults to an empty string.
     */
    val uploadPath: Property<String>
}


private abstract class DefaultCustomHelmPublishingRepository
@Inject constructor(
    name: String,
    objects: ObjectFactory
) : AbstractHelmPublishingRepository(objects, name), CustomHelmPublishingRepository {

    override val publisherParams: PublisherParams
        get() = CustomPublisherParams(
            url = url.get(),
            credentials = configuredCredentials.orNull?.toSerializable(),
            uploadMethod = uploadMethod.get(),
            uploadPath = uploadPath.get()
        )


    private class CustomPublisherParams(
        private val url: URI,
        private val credentials: SerializableCredentials?,
        private val uploadMethod: String,
        private val uploadPath: String
    ) : PublisherParams {

        override fun createPublisher(): HelmChartPublisher =
            CustomPublisher(url, credentials, uploadMethod, uploadPath)
    }


    private class CustomPublisher(
        url: URI,
        credentials: SerializableCredentials?,
        override val uploadMethod: String,
        private val uploadPath: String
    ) : AbstractHttpHelmChartPublisher(url, credentials) {

        override fun uploadPath(chartName: String, chartVersion: String): String =
            this.uploadPath
                .replace("{name}", chartName)
                .replace("{version}", chartVersion)
                .replace("{filename}", "$chartName-$chartVersion.tgz")
    }


    init {
        uploadMethod.convention("POST")
        uploadPath.convention("")
    }
}


internal fun ObjectFactory.newCustomHelmPublishingRepository(name: String): CustomHelmPublishingRepository =
    newInstance(DefaultCustomHelmPublishingRepository::class.java, name)
