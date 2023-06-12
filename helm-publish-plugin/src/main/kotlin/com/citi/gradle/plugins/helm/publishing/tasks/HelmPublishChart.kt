package com.citi.gradle.plugins.helm.publishing.tasks

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import com.citi.gradle.plugins.helm.HELM_GROUP
import com.citi.gradle.plugins.helm.publishing.dsl.HelmPublishingRepository
import com.citi.gradle.plugins.helm.publishing.dsl.HelmPublishingRepositoryInternal
import com.citi.gradle.plugins.helm.publishing.publishers.PublisherParams
import org.unbrokendome.gradle.pluginutils.property


/**
 * Publishes a packaged helm chart to a repository.
 */
open class HelmPublishChart
@Inject constructor(
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {
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

        val publisherParams = checkNotNull(targetRepository) { "Target repository must be set." }
            .let { targetRepository ->
                (targetRepository as HelmPublishingRepositoryInternal).publisherParams
            }

        workerExecutor.noIsolation().submit(PublishChartWorkAction::class.java) { params ->
            params.chartName.set(chartName)
            params.chartVersion.set(chartVersion)
            params.chartFile.set(chartFile)
            params.publisherParams.set(publisherParams)
        }
    }
}


internal interface PublishChartWorkParameters : WorkParameters {

    val chartName: Property<String>

    val chartVersion: Property<String>

    val chartFile: RegularFileProperty

    val publisherParams: Property<PublisherParams>
}


internal abstract class PublishChartWorkAction : WorkAction<PublishChartWorkParameters> {

    override fun execute() {

        val parameters = this.parameters
        val publisherParams = parameters.publisherParams.get()
        val chartName = parameters.chartName.get()
        val chartVersion = parameters.chartVersion.get()
        val chartFile = parameters.chartFile.get().asFile

        val publisher = publisherParams.createPublisher()

        publisher.publish(
            chartName = chartName,
            chartVersion = chartVersion,
            chartFile = chartFile
        )
    }
}
