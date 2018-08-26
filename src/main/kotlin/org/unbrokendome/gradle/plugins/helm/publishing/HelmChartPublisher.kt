package org.unbrokendome.gradle.plugins.helm.publishing

import java.io.File


/**
 * Strategy to publish a packaged Helm chart to a repository.
 */
internal interface HelmChartPublisher {

    /**
     * Publishes the given chart file to the repository.
     *
     * @param chartFile the path to the chart package file (`.tgz`) on disk
     */
    fun publish(chartFile: File)
}
