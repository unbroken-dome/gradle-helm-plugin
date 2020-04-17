package org.unbrokendome.gradle.plugins.helm.publishing.publishers


internal class HttpResponseException(
    requestMethod: String,
    requestUrl: String,
    responseCode: Int,
    responseReasonPhrase: String
) : Exception(
    "Chart repository server returned error response for $requestMethod $requestUrl: " +
            "$responseCode $responseReasonPhrase"
)
