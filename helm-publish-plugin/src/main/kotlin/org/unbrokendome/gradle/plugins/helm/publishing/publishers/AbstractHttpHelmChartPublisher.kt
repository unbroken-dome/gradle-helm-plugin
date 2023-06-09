package com.citi.gradle.plugins.helm.publishing.publishers

import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.slf4j.LoggerFactory
import com.citi.gradle.plugins.helm.dsl.credentials.internal.SerializableCertificateCredentials
import com.citi.gradle.plugins.helm.dsl.credentials.internal.SerializableCredentials
import com.citi.gradle.plugins.helm.dsl.credentials.internal.SerializablePasswordCredentials
import java.io.File
import java.net.URI


/**
 * Publishes Helm charts to a remote repository using HTTP.
 */
internal abstract class AbstractHttpHelmChartPublisher(
    private val url: URI,
    private val credentials: SerializableCredentials? = null
) : HelmChartPublisher {

    private val logger = LoggerFactory.getLogger(javaClass)


    protected abstract val uploadMethod: String


    protected abstract fun uploadPath(chartName: String, chartVersion: String): String

    protected open fun additionalHeaders(
        chartName: String,
        chartVersion: String,
        chartFile: File
    ): Map<String, String> =
        emptyMap()

    protected open fun requestBody(chartFile: File): RequestBody = chartFile.asRequestBody(MEDIA_TYPE_GZIP)


    protected companion object {
        val MEDIA_TYPE_GZIP: MediaType = "application/x-gzip".toMediaType()
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

        logger.info(
            "Uploading chart file {} via {} to repository URL {}",
            chartFile, uploadMethod, uploadUrl
        )

        val httpClient = createHttpClient()

        val request = Request.Builder().run {
            url(uploadUrl.toHttpUrl())
            method(uploadMethod, requestBody(chartFile))
            build()
        }

        httpClient.newCall(request)
            .execute()
            .use { response ->
                if (response.code !in 200..299) {
                    throw HttpResponseException(uploadMethod, uploadUrl, response.code, response.message)
                }
            }
    }


    private fun createHttpClient(): OkHttpClient =
        OkHttpClient.Builder().run {

            if (credentials != null) {
                when (credentials) {
                    is SerializablePasswordCredentials ->
                        addInterceptor(credentials.createAuthInterceptor())

                    is SerializableCertificateCredentials -> {
                        val handshakeCertificates = HandshakeCertificates.Builder()
                            .heldCertificate(credentials.createHeldCertificate())
                            .addPlatformTrustedCertificates()
                            .build()
                        sslSocketFactory(handshakeCertificates.sslSocketFactory(), handshakeCertificates.trustManager)
                    }
                }
            }

            build()
        }


    private fun SerializablePasswordCredentials.createAuthInterceptor(): Interceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Authorization", Credentials.basic(username, password.orEmpty()))
            .build()
        chain.proceed(request)
    }


    private fun SerializableCertificateCredentials.createHeldCertificate(): HeldCertificate {
        val certText = certificateFile.readText()
        val keyText = keyFile.readText()
        return HeldCertificate.decode("$certText\n$keyText")
    }
}
