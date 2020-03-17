package org.unbrokendome.gradle.plugins.helm.rules

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer


internal abstract class AbstractTaskRule<S : Named, T : Task>(
    taskType: Class<T>,
    protected val tasks: TaskContainer,
    sourceContainer: NamedDomainObjectCollection<S>,
    namePattern: RuleNamePattern
) : AbstractPatternRule<S, T>(taskType, tasks, sourceContainer, namePattern)


internal abstract class AbstractTaskRule2<S1 : Named, S2 : Named, T : Task>(
    taskType: Class<T>,
    protected val tasks: TaskContainer,
    sourceContainer1: NamedDomainObjectCollection<S1>,
    sourceContainer2: NamedDomainObjectCollection<S2>,
    namePattern: RuleNamePattern2
) : AbstractPatternRule2<S1, S2, T>(taskType, tasks, sourceContainer1, sourceContainer2, namePattern)
