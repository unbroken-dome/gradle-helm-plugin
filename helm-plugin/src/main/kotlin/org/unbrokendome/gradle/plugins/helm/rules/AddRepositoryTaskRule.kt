package com.citi.gradle.plugins.helm.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.tasks.TaskContainer
import com.citi.gradle.plugins.helm.command.tasks.HelmAddRepository
import com.citi.gradle.plugins.helm.dsl.HelmRepository
import com.citi.gradle.plugins.helm.dsl.credentials.CertificateCredentials
import com.citi.gradle.plugins.helm.dsl.credentials.PasswordCredentials
import org.unbrokendome.gradle.pluginutils.ifPresent
import org.unbrokendome.gradle.pluginutils.rules.AbstractTaskRule
import org.unbrokendome.gradle.pluginutils.rules.RuleNamePattern


private val namePattern =
    RuleNamePattern.parse("helmAdd<Repo>Repository")


/**
 * The name of the [HelmAddRepository] task that registers this repository.
 */
val HelmRepository.registerTaskName: String
    get() = namePattern.mapName(name)


/**
 * A rule that creates an [HelmAddRepository] task for a configured repository.
 */
internal class AddRepositoryTaskRule(
    tasks: TaskContainer,
    repositories: NamedDomainObjectCollection<HelmRepository>
) : AbstractTaskRule<HelmRepository, HelmAddRepository>(
    HelmAddRepository::class.java, tasks, repositories,
    namePattern
) {

    override fun HelmAddRepository.configureFrom(source: HelmRepository) {
        description = "Registers the ${source.name} repository."
        repositoryName.set(source.name)
        url.set(source.url)
        caFile.set(source.caFile)

        source.configuredCredentials.ifPresent { credentials ->
            when (credentials) {
                is PasswordCredentials -> {
                    username.set(credentials.username)
                    password.set(credentials.password)
                }
                is CertificateCredentials -> {
                    certificateFile.set(credentials.certificateFile)
                    keyFile.set(credentials.keyFile)
                }
                else ->
                    throw IllegalArgumentException(
                        "Only PasswordCredentials and " +
                                "CertificateCredentials are supported for Helm repositories"
                    )
            }
        }
    }
}
