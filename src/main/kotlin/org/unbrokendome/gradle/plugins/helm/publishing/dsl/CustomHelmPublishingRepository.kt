package org.unbrokendome.gradle.plugins.helm.publishing.dsl

import org.gradle.api.credentials.Credentials
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.unbrokendome.gradle.plugins.helm.publishing.publishers.AbstractHttpHelmChartPublisher
import org.unbrokendome.gradle.plugins.helm.publishing.publishers.HelmChartPublisher
import org.unbrokendome.gradle.plugins.helm.util.property
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


private open class DefaultCustomHelmPublishingRepository
@Inject constructor(
    name: String,
    objects: ObjectFactory
) : AbstractHelmPublishingRepository(objects, name), CustomHelmPublishingRepository {

    override val uploadMethod: Property<String> =
        objects.property<String>()
            .convention("POST")


    override val uploadPath: Property<String> =
        objects.property<String>()
            .convention("")


    override val publisher: HelmChartPublisher
        get() = Publisher(url.get(), configuredCredentials, uploadMethod.get(), uploadPath.get())


    private class Publisher(
        url: URI,
        credentials: Provider<Credentials>,
        override val uploadMethod: String,
        private val uploadPath: String
    ) : AbstractHttpHelmChartPublisher(url, credentials) {

        override fun uploadPath(chartName: String, chartVersion: String): String =
            this.uploadPath
                .replace("{name}", chartName)
                .replace("{version}", chartVersion)
                .replace("{filename}", "$chartName-$chartVersion.tgz")
    }
}


internal fun ObjectFactory.newCustomHelmPublishingRepository(name: String): CustomHelmPublishingRepository =
    newInstance(DefaultCustomHelmPublishingRepository::class.java, name)
