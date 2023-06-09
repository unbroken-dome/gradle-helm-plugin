package com.citi.gradle.plugins.helm.command.tasks

import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import com.citi.gradle.plugins.helm.util.withLockFile
import org.unbrokendome.gradle.pluginutils.listProperty
import org.unbrokendome.gradle.pluginutils.property
import java.time.Duration


/**
 * Gets the latest information about charts from chart repositories.
 *
 * Corresponds to the `helm repo update` CLI command.
 */
open class HelmUpdateRepositories : AbstractHelmCommandTask() {

    /**
     * The names of configured repositories.
     */
    @get:Internal
    val repositoryNames: ListProperty<String> =
        project.objects.listProperty()


    /**
     * Lock file to synchronize multiple [HelmUpdateRepositories] tasks that access the same cache directory.
     */
    private val taskSyncLockFile: Provider<RegularFile> =
        repositoryCacheDir.map { it.file("gradle-helm-update.lock") }


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
        // skip the task if we don't have any repositories configured in the project
        @Suppress("LeakingThis")
        onlyIf { !repositoryNames.orNull.isNullOrEmpty() }

        // declare the repositories.yaml file as a skip-when-empty input, so we get a
        // "no source" result if it doesn't exist (better than "up to date")
        inputs.files(repositoryConfigFile)
            .withPropertyName("repositoryConfigFile")
            .skipWhenEmpty()

        outputs.upToDateWhen { task -> checkUpToDate(task) }
    }


    @TaskAction
    fun updateRepositories() {

        val lockFile = this.taskSyncLockFile.get().asFile
        withLockFile(lockFile) {

            // Do the up-to-date check again, it might be that another task operating on the
            // same cache directory updated it in the meantime
            if (checkUpToDate(this)) {
                didWork = false

            } else {
                execHelm("repo", "update")
            }
        }
    }


    private fun checkUpToDate(task: Task): Boolean {

        val repositoryConfigFile = project.file(this.repositoryConfigFile)
        if (!repositoryConfigFile.exists()) {
            // this should usually not happen, but better to check to avoid an exception later
            logger.debug("{} is up-to-date because the repository config file does not exist.", task)
            return true
        }

        val repositoryCacheDir = project.file(this.repositoryCacheDir)
        if (!repositoryCacheDir.isDirectory) {
            logger.debug("{} is not up-to-date because the repository cache directory does not exist.", task)
            return false
        }

        if (repositoryCacheTtl.get() == Duration.ZERO) {
            logger.debug("{} is not up-to-date because the repository cache TTL is set to 0.", task)
            return false
        }

        val filesToCheck = this.repositoryNames.get().asSequence()
            .flatMap { sequenceOf("${it}-charts.txt", "${it}-index.yaml") }
            .map { repositoryCacheDir.resolve(it) }
            .toList()

        val (existingFiles, nonExistingFiles) = filesToCheck.partition { it.exists() }
        if (nonExistingFiles.any()) {
            if (logger.isDebugEnabled) {
                logger.debug(
                    "{} is not up-to-date because the following repository cache files are not present: {}",
                    task, nonExistingFiles.map { it.name }
                )
            }
            return false
        }

        // Find any cache files that are either older than the config file,
        // or have been around for longer than the cache TTL
        val minLastModTime = maxOf(
            System.currentTimeMillis() - repositoryCacheTtl.get().toMillis(),
            repositoryConfigFile.lastModified()
        )
        val outdatedFiles = existingFiles.filter { it.lastModified() < minLastModTime }
        if (outdatedFiles.any()) {
            if (logger.isDebugEnabled) {
                logger.debug(
                    "{} is not up-to-date because the following repository cache files are outdated: {}",
                    task, outdatedFiles.map { it.name }
                )
            }
            return false
        }

        return true
    }
}
