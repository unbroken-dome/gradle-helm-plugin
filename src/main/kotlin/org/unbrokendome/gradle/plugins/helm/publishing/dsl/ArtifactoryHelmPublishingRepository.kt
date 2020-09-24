package org.unbrokendome.gradle.plugins.helm.publishing.dsl

import org.gradle.api.model.ObjectFactory
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.SerializableCredentials
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.toSerializable
import org.unbrokendome.gradle.plugins.helm.publishing.publishers.AbstractHttpHelmChartPublisher
import org.unbrokendome.gradle.plugins.helm.publishing.publishers.HelmChartPublisher
import org.unbrokendome.gradle.plugins.helm.publishing.publishers.PublisherParams
import org.unbrokendome.gradle.plugins.helm.util.calculateDigestHex
import java.io.File
import java.net.URI
import javax.inject.Inject


interface ArtifactoryHelmPublishingRepository : HelmPublishingRepository


private abstract class DefaultArtifactoryHelmPublishingRepository
@Inject constructor(
    name: String,
    objects: ObjectFactory
) : AbstractHelmPublishingRepository(objects, name), ArtifactoryHelmPublishingRepository {

    override val publisherParams: PublisherParams
        get() = ArtifactoryPublisherParams(
            url = url.get(),
            credentials = configuredCredentials.orNull?.toSerializable()
        )


    private class ArtifactoryPublisherParams(
        private val url: URI,
        private val credentials: SerializableCredentials?
    ) : PublisherParams {

        override fun createPublisher(): HelmChartPublisher =
            ArtifactoryPublisher(url, credentials)
    }


    private class ArtifactoryPublisher(
        url: URI,
        credentials: SerializableCredentials?
    ) : AbstractHttpHelmChartPublisher(url, credentials) {

        override val uploadMethod: String
            get() = "PUT"


        override fun uploadPath(chartName: String, chartVersion: String): String =
            "/$chartName-$chartVersion.tgz"


        override fun additionalHeaders(chartName: String, chartVersion: String, chartFile: File): Map<String, String> =
            mapOf(
                "X-Checksum-Sha1" to calculateDigestHex(chartFile.toPath(), "SHA-1"),
                "X-Checksum-Sha256" to calculateDigestHex(chartFile.toPath(), "SHA-256"),
                "X-Checksum-Md5" to calculateDigestHex(chartFile.toPath(), "MD5")
            )
    }
}


internal fun ObjectFactory.newArtifactoryHelmPublishingRepository(name: String): ArtifactoryHelmPublishingRepository =
    newInstance(DefaultArtifactoryHelmPublishingRepository::class.java, name)
