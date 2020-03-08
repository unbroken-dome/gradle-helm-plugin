package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.unbrokendome.gradle.plugins.helm.util.property
import java.time.Duration


/**
 * Gets the latest information about charts from chart repositories.
 *
 * Corresponds to the `helm repo update` CLI command.
 */
open class HelmUpdateRepositories : AbstractHelmCommandTask() {

    /**
     * Defines how long the repository cache is considered up-to-date after it was last written.
     *
     * If the repository cache was updated within this timeframe, and the repository config file was not changed,
     * then the task will be considered up-to-date. This is intended to avoid large downloads on every Gradle build.
     *
     * Defaults to one hour. Set the property to [Duration.ZERO] to effectively disable the cache-TTL behavior.
     */
    @get:Internal
    val repositoryCacheTtl: Property<Duration> =
        project.objects.property<Duration>()
            .convention(Duration.ofHours(1))


    init {
        inputs.file(repositoryConfigFile)
            .withPropertyName("repositoryConfigFile")
            .optional().skipWhenEmpty()
        outputs.dir(repositoryCacheDir)
            .withPropertyName("repositoryCacheDir")
            .optional()


        outputs.upToDateWhen { task -> checkUpToDate(task) }
    }


    private fun checkUpToDate(task: Task): Boolean {

        val repositoryCacheDir = project.file(this.repositoryCacheDir)
        if (!repositoryCacheDir.isDirectory) {
            logger.debug("{} is not up-to-date because the repository cache directory does not exist.", task)
            return false
        }

        if (repositoryCacheTtl.get() == Duration.ZERO) {
            logger.debug("{} is not up-to-date because the repository cache TTL is set to 0.", task)
            return false
        }

        val minLastModTime = System.currentTimeMillis() - repositoryCacheTtl.get().toMillis()
        val outdatedCacheFiles = repositoryCacheDir.listFiles { file -> file.lastModified() < minLastModTime }.orEmpty()

        if (outdatedCacheFiles.any()) {
            logger.debug("{} is not up-to-date because the following repository cache files are outdated: {}",
            task, outdatedCacheFiles.contentToString()
            )
            return false
        }

        return true
    }


    @TaskAction
    fun updateRepositories() {
        execHelm("repo", "update")
    }
}
