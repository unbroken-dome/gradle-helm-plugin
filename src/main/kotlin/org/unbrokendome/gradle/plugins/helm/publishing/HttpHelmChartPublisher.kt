package org.unbrokendome.gradle.plugins.helm.publishing

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
internal class HttpHelmChartPublisher(
        url: URI,
        private val uploadMethod: String = "POST",
        uploadPath: String? = "/api/charts",
        private val credentials: Provider<Credentials> = Providers.notDefined())
    : HelmChartPublisher {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val fullUploadUrl = buildFullUploadUrl(url.toString(), uploadPath)


    private companion object {
        val CONTENT_TYPE_GZIP: ContentType = ContentType.create("application/x-gzip")

        fun buildFullUploadUrl(url: String, uploadPath: String?): String {
            return uploadPath.orEmpty().removePrefix("/").let { path ->
                StringBuilder(url)
                        .apply {
                            if (!url.endsWith("/") && path.isNotEmpty()) {
                                append("/")
                            }
                        }
                        .append(path)
                        .toString()
            }
        }
    }


    override fun publish(chartFile: File) {

        createHttpClient().use { httpClient ->

            val request = RequestBuilder.create(uploadMethod)
                    .setUri(fullUploadUrl)
                    .setEntity(FileEntity(chartFile, CONTENT_TYPE_GZIP))
                    .build()

            logger.info("Uploading chart file {} via {} to repository URL {}",
                    chartFile, uploadMethod, fullUploadUrl)

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
