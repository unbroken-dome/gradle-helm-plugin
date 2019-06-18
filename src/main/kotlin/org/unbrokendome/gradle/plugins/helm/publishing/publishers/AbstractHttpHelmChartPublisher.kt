package org.unbrokendome.gradle.plugins.helm.publishing.publishers

import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.ContentType
import org.apache.http.entity.FileEntity
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.BasicResponseHandler
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.gradle.api.credentials.Credentials
import org.gradle.api.internal.provider.Providers
import org.gradle.api.provider.Provider
import org.slf4j.LoggerFactory
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.CertificateCredentials
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.PasswordCredentials
import org.unbrokendome.gradle.plugins.helm.util.ifPresent
import java.io.File
import java.net.URI


/**
 * Publishes Helm charts to a remote repository using HTTP.
 */
internal abstract class AbstractHttpHelmChartPublisher(
    private val url: URI,
    private val credentials: Provider<Credentials> = Providers.notDefined()
) : HelmChartPublisher {

    private val logger = LoggerFactory.getLogger(javaClass)


    protected abstract val uploadMethod: String


    protected abstract fun uploadPath(chartName: String, chartVersion: String): String


    protected open fun additionalHeaders(chartName: String, chartVersion: String, chartFile: File): Map<String, String> =
        emptyMap()


    private companion object {
        val CONTENT_TYPE_GZIP: ContentType = ContentType.create("application/x-gzip")
    }


    private fun buildFullUploadUrl(chartName: String, chartVersion: String): String {
        val path = this.uploadPath(chartName, chartVersion).removePrefix("/")
        return buildString {
            append(url.toString())
            if (!endsWith('/') && path.isNotEmpty()) {
                append('/')
            }
            append(path)
        }
    }


    final override fun publish(chartName: String, chartVersion: String, chartFile: File) {

        val uploadUrl = buildFullUploadUrl(chartName, chartVersion)

        createHttpClient().use { httpClient ->

            val request = RequestBuilder.create(uploadMethod)
                .setUri(uploadUrl)
                .also { b ->
                    additionalHeaders(chartName, chartVersion, chartFile).forEach { (key, value) ->
                        b.addHeader(key, value)
                    }
                }
                .setEntity(FileEntity(chartFile, CONTENT_TYPE_GZIP))
                .build()

            logger.info(
                "Uploading chart file {} via {} to repository URL {}",
                chartFile, uploadMethod, uploadUrl
            )

            httpClient.execute(request, BasicResponseHandler())
        }
    }


    private fun createHttpClient(): CloseableHttpClient {
        return HttpClientBuilder.create()
            .apply {

                credentials.ifPresent { credentials ->
                    when (credentials) {
                        is PasswordCredentials ->
                            BasicCredentialsProvider()
                                .apply {
                                    setCredentials(AuthScope.ANY, credentials.toHttpClientCredentials())
                                }
                                .let(this::setDefaultCredentialsProvider)

                        is CertificateCredentials ->
                            TODO("Authentication by client certificates is not yet implemented")

                        else ->
                            throw IllegalStateException("Unsupported credentials type: ${credentials.javaClass.name}")
                    }
                }
            }
            .build()
    }


    private fun PasswordCredentials.toHttpClientCredentials(): UsernamePasswordCredentials =
        UsernamePasswordCredentials(username.orNull, password.orNull)
}
