package com.citi.gradle.plugins.helm.command.rules

import com.citi.gradle.plugins.helm.command.tasks.HelmDownloadClientPackage
import java.net.URI
import org.gradle.api.Project
import org.gradle.api.Rule
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider


/**
 * A rule that creates a [HelmDownloadClientPackage] task for a given version.
 */
internal class DownloadClientPackageTaskRule(
    private val project: Project,
    private val baseUrl: Provider<URI>,
    private val downloadDir: Provider<Directory>
) : Rule {

    private val regex = Regex('^' + downloadClientTaskName("(.*)") + '$')


    override fun getDescription(): String =
        downloadClientTaskName("<version>")


    override fun apply(domainObjectName: String) {
        val matchResult = regex.matchEntire(domainObjectName) ?: return
        val helmClientVersion = matchResult.groupValues[1]

        project.tasks.create(domainObjectName, HelmDownloadClientPackage::class.java) { task ->
            task.version.set(helmClientVersion)
            task.baseUrl.set(baseUrl)
            task.destinationDir.set(downloadDir)
        }
    }
}


/**
 * Gets the name of the [HelmDownloadClientPackage] task for the given version.
 *
 * @param version the Helm client version
 * @return the task name
 */
internal fun downloadClientTaskName(version: String): String =
    "helmDownloadClient_$version"
