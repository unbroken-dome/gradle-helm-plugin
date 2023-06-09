package com.citi.gradle.plugins.helm.publishing.dsl

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
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


interface GitlabHelmPublishingRepository : HelmPublishingRepository {
    /**
     * The ID of the Gitlab project.
     */
    val projectId: Property<Int>
}


private open class DefaultGitlabHelmPublishingRepository @Inject constructor(
    name: String,
    objects: ObjectFactory
) : AbstractHelmPublishingRepository(objects, name), GitlabHelmPublishingRepository {

    override val projectId: Property<Int> =
        objects.property()

    override val publisherParams: PublisherParams
        get() = GitlabPublisherParams(
            url = requireNotNull(url.orNull) { "url is required for Gitlab publishing repository" },
            credentials = configuredCredentials.orNull?.toSerializable(),
            projectId = requireNotNull(projectId.orNull) { "projectId is required for Gitlab publishing repository" }
        )


    private class GitlabPublisherParams(
        private val url: URI,
        private val credentials: SerializableCredentials?,
        private var projectId: Int
    ) : PublisherParams {

        override fun createPublisher(): HelmChartPublisher =
            GitlabPublisher(url, credentials, projectId)
    }


    private class GitlabPublisher(
        url: URI,
        credentials: SerializableCredentials?,
        private val projectId: Int
    ) : AbstractHttpHelmChartPublisher(url, credentials) {

        override val uploadMethod: String
            get() = "POST"

        override fun uploadPath(chartName: String, chartVersion: String): String =
            "/projects/$projectId/packages/helm/api/stable/charts"

        override fun requestBody(chartFile: File): RequestBody =
            MultipartBody.Builder().run {
                setType(MultipartBody.FORM)
                addFormDataPart("chart", chartFile.name, chartFile.asRequestBody(MEDIA_TYPE_GZIP))
                build()
            }
    }
}


internal fun ObjectFactory.newGitlabHelmPublishingRepository(name: String): GitlabHelmPublishingRepository =
    newInstance(DefaultGitlabHelmPublishingRepository::class.java, name)
