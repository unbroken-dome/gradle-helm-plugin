package com.citi.gradle.plugins.helm.release.rules

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import com.citi.gradle.plugins.helm.release.dsl.HelmRelease
import com.citi.gradle.plugins.helm.release.dsl.HelmReleaseTarget
import org.unbrokendome.gradle.pluginutils.rules.AbstractTaskRule2
import org.unbrokendome.gradle.pluginutils.rules.RuleNamePattern2


internal abstract class AbstractHelmReleaseToTargetTaskRule<T : Task>(
    taskType: Class<T>,
    tasks: TaskContainer,
    releases: NamedDomainObjectCollection<HelmRelease>,
    releaseTargets: NamedDomainObjectCollection<HelmReleaseTarget>,
    namePattern: RuleNamePattern2
) : AbstractTaskRule2<HelmRelease, HelmReleaseTarget, T>(
    taskType, tasks, releases, releaseTargets, namePattern
) {

    protected val releases: NamedDomainObjectCollection<HelmRelease>
        get() = sourceContainer1


    protected val releaseTargets: NamedDomainObjectCollection<HelmReleaseTarget>
        get() = sourceContainer2
}
