package com.citi.gradle.plugins.helm.publishing.publishers

import java.io.File


/**
 * Strategy to publish a packaged Helm chart to a repository.
 */
internal interface HelmChartPublisher {

    /**
     * Publishes the given chart file to the repository.
     *
     * @param chartName the name of the chart
     * @param chartVersion the version of the chart
     * @param chartFile the path to the chart package file (`.tgz`) on disk
     * @throws HttpResponseException if the response code indicates an error
     */
    fun publish(chartName: String, chartVersion: String, chartFile: File)
}
