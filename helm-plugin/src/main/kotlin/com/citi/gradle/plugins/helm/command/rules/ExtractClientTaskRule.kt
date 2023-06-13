package com.citi.gradle.plugins.helm.command.rules

import com.citi.gradle.plugins.helm.command.tasks.HelmDownloadClientPackage
import com.citi.gradle.plugins.helm.command.tasks.HelmExtractClient
import org.gradle.api.Project
import org.gradle.api.Rule
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider


/**
 * A rule that creates a [HelmExtractClient] task for a given version.
 */
internal class ExtractClientTaskRule(
    private val project: Project,
    private val baseExtractDir: Provider<Directory>
) : Rule {

    private val regex = Regex('^' + extractClientTaskName("(.*)") + '$')


    override fun getDescription(): String =
        extractClientTaskName("<version>")


    override fun apply(domainObjectName: String) {
        val matchResult = regex.matchEntire(domainObjectName) ?: return
        val helmClientVersion = matchResult.groupValues[1]

        val downloadTask = project.tasks.named(
            downloadClientTaskName(helmClientVersion), HelmDownloadClientPackage::class.java
        )

        project.tasks.create(domainObjectName, HelmExtractClient::class.java) { task ->
            task.dependsOn(downloadTask)
            task.version.set(helmClientVersion)
            task.archiveFile.set(
                downloadTask.flatMap { it.outputFile }
            )
            task.baseDestinationDir.set(baseExtractDir)
        }
    }
}


/**
 * Gets the name of the [HelmExtractClient] task for the given version.
 *
 * @param version the Helm client version
 * @return the task name
 */
internal fun extractClientTaskName(version: String): String =
    "helmExtractClient_$version"
