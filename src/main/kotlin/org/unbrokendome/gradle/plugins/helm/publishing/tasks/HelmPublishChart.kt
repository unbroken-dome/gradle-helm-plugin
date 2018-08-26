package org.unbrokendome.gradle.plugins.helm.publishing.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.unbrokendome.gradle.plugins.helm.HELM_GROUP
import org.unbrokendome.gradle.plugins.helm.publishing.dsl.HelmPublishingRepository
import org.unbrokendome.gradle.plugins.helm.publishing.dsl.HelmPublishingRepositoryInternal


/**
 * Publishes a packaged helm chart to a repository.
 */
open class HelmPublishChart : DefaultTask() {

    init {
        group = HELM_GROUP
    }


    /**
     * The chart package file to be published.
     */
    @get:InputFile
    @Suppress("LeakingThis")
    val chartFile: RegularFileProperty =
            newInputFile()


    /**
     * The target repository.
     */
    @get:Internal
    var targetRepository: HelmPublishingRepository? = null


    /**
     * Publishes the chart.
     */
    @TaskAction
    fun publish() {
        checkNotNull(targetRepository) { "Target repository must be set." }
                .let { targetRepository ->
                    (targetRepository as HelmPublishingRepositoryInternal)
                            .publisher
                            .publish(chartFile.get().asFile)
                }
    }
}
