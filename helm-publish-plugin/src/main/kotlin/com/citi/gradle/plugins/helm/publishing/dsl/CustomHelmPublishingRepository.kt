package com.citi.gradle.plugins.helm.publishing.dsl

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import com.citi.gradle.plugins.helm.dsl.credentials.internal.SerializableCredentials
import com.citi.gradle.plugins.helm.dsl.credentials.internal.toSerializable
import com.citi.gradle.plugins.helm.publishing.publishers.AbstractHttpHelmChartPublisher
import com.citi.gradle.plugins.helm.publishing.publishers.HelmChartPublisher
import com.citi.gradle.plugins.helm.publishing.publishers.PublisherParams
import com.citi.gradle.plugins.helm.publishing.util.toMultipartBody
import org.unbrokendome.gradle.pluginutils.property
import java.io.File
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

    /**
     * Indicates to use a multipart form for publishing.
     *
     * Defaults to `false`.
     */
    val multipartForm: Property<Boolean>
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

    override val multipartForm: Property<Boolean> =
        objects.property<Boolean>()
            .convention(false)

    override val publisherParams: PublisherParams
        get() = CustomPublisherParams(
            url = url.get(),
            credentials = configuredCredentials.orNull?.toSerializable(),
            uploadMethod = uploadMethod.get(),
            uploadPath = uploadPath.get(),
            multipartForm = multipartForm.get()
        )


    private class CustomPublisherParams(
        private val url: URI,
        private val credentials: SerializableCredentials?,
        private val uploadMethod: String,
        private val uploadPath: String,
        private val multipartForm: Boolean
    ) : PublisherParams {

        override fun createPublisher(): HelmChartPublisher =
            CustomPublisher(url, credentials, uploadMethod, uploadPath, multipartForm)
    }

    private class CustomPublisher(
        url: URI,
        credentials: SerializableCredentials?,
        override val uploadMethod: String,
        private val uploadPath: String,
        private val multipartForm: Boolean
    ) : AbstractHttpHelmChartPublisher(url, credentials) {

        override fun uploadPath(chartName: String, chartVersion: String): String =
            this.uploadPath
                .replace("{name}", chartName)
                .replace("{version}", chartVersion)
                .replace("{filename}", "$chartName-$chartVersion.tgz")

        override fun requestBody(chartFile: File) = super.requestBody(chartFile).let {
            it.takeUnless { multipartForm } ?: it.toMultipartBody(chartFile.name)
        }
    }
}


internal fun ObjectFactory.newCustomHelmPublishingRepository(name: String): CustomHelmPublishingRepository =
    newInstance(DefaultCustomHelmPublishingRepository::class.java, name)
