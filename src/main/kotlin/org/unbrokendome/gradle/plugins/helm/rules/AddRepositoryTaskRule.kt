package org.unbrokendome.gradle.plugins.helm.rules

import org.gradle.api.tasks.TaskContainer
import org.unbrokendome.gradle.plugins.helm.HelmPlugin
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmAddRepository
import org.unbrokendome.gradle.plugins.helm.dsl.HelmRepository
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.CertificateCredentials
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.PasswordCredentials
import org.unbrokendome.gradle.plugins.helm.util.ifPresent


/**
 * A rule that creates an [HelmAddRepository] task for a configured repository.
 */
internal class AddRepositoryTaskRule(
    private val tasks: TaskContainer,
    private val repositories: Iterable<HelmRepository>
) : AbstractRule() {

    internal companion object {
        fun getTaskName(repositoryName: String) =
            "helmAdd${repositoryName.capitalize()}Repository"
    }


    private val regex = Regex(getTaskName("(\\p{Upper}.*)"))


    override fun getDescription(): String =
        "Pattern: ${getTaskName("<Repository>")}"


    override fun apply(taskName: String) {
        if (regex.matches(taskName)) {
            repositories.find { it.registerTaskName == taskName }
                ?.let { repository ->
                    tasks.create(taskName, HelmAddRepository::class.java) { task ->
                        task.description = "Registers the ${repository.name} repository."
                        task.repositoryName.set(repository.name)
                        task.url.set(repository.url)
                        task.caFile.set(repository.caFile)

                        repository.configuredCredentials.ifPresent { credentials ->
                            when (credentials) {
                                is PasswordCredentials -> {
                                    task.username.set(credentials.username)
                                    task.password.set(credentials.password)
                                }
                                is CertificateCredentials -> {
                                    task.certificateFile.set(credentials.certificateFile)
                                    task.keyFile.set(credentials.keyFile)
                                }
                                else ->
                                    throw IllegalArgumentException(
                                        "Only PasswordCredentials and " +
                                                "CertificateCredentials are supported for Helm repositories"
                                    )
                            }
                        }

                        task.dependsOn(HelmPlugin.initClientTaskName)
                    }
                }
        }
    }
}


/**
 * The name of the [HelmAddRepository] task that registers this repository.
 */
val HelmRepository.registerTaskName: String
    get() = AddRepositoryTaskRule.getTaskName(name)
