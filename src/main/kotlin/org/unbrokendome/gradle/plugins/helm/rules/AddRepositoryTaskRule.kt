package org.unbrokendome.gradle.plugins.helm.rules

import org.gradle.api.tasks.TaskContainer
import org.unbrokendome.gradle.plugins.helm.HelmPlugin
import org.unbrokendome.gradle.plugins.helm.dsl.HelmRepository
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmAddRepository


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
                            task.dependsOn(HelmPlugin.initClientTaskName)
                        }
                    }
        }
    }
}


/**
 * The name of the [HelmAddRepository] task that registers this repository.
 */
internal val HelmRepository.registerTaskName: String
        get() = AddRepositoryTaskRule.getTaskName(name)
