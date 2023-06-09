package com.citi.gradle.plugins.helm.release.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import com.citi.gradle.plugins.helm.release.dsl.HelmRelease
import org.unbrokendome.gradle.pluginutils.rules.AbstractTaskRule
import org.unbrokendome.gradle.pluginutils.rules.RuleNamePattern


internal abstract class AbstractHelmReleaseTaskRule<T : Task>(
    taskType: Class<T>,
    tasks: TaskContainer,
    releases: NamedDomainObjectCollection<HelmRelease>,
    namePattern: RuleNamePattern
) : AbstractTaskRule<HelmRelease, T>(taskType, tasks, releases, namePattern) {

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    abstract override fun T.configureFrom(release: HelmRelease)


    protected val releases: NamedDomainObjectCollection<HelmRelease>
        get() = sourceContainer
}
