package org.unbrokendome.gradle.plugins.helm.publishing.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.unbrokendome.gradle.plugins.helm.HELM_GROUP
import org.unbrokendome.gradle.plugins.helm.publishing.dsl.HelmPublishingRepository
import org.unbrokendome.gradle.plugins.helm.publishing.dsl.HelmPublishingRepositoryInternal
import org.unbrokendome.gradle.plugins.helm.util.property


/**
 * Publishes a packaged helm chart to a repository.
 */
open class HelmPublishChart : DefaultTask() {

    init {
        group = HELM_GROUP
    }


    /**
     * The chart name.
     */
    @get:Input
    val chartName: Property<String> =
        project.objects.property()


    /**
     * The chart version.
     */
    @get:Input
    val chartVersion: Property<String> =
        project.objects.property()


    /**
     * The chart package file to be published.
     */
    @get:InputFile
    val chartFile: RegularFileProperty =
        project.objects.fileProperty()


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
                    .publish(chartName.get(), chartVersion.get(), project.file(chartFile))
            }
    }
}
