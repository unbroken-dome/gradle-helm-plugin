package com.citi.gradle.plugins.helm.release.rules

import org.gradle.api.NamedDomainObjectContainer
import com.citi.gradle.plugins.helm.HELM_DEFAULT_RELEASE_TARGET
import com.citi.gradle.plugins.helm.release.dsl.HelmReleaseTarget
import org.unbrokendome.gradle.pluginutils.rules.AbstractRule


/**
 * Creates a release target named "default" which has the global default properties.
 */
internal class DefaultReleaseTargetRule(
    private val releaseTargets: NamedDomainObjectContainer<HelmReleaseTarget>
) : AbstractRule() {

    override fun getDescription(): String =
        "default release target"


    override fun apply(domainObjectName: String) {
        if (domainObjectName == HELM_DEFAULT_RELEASE_TARGET && releaseTargets.isEmpty()) {
            releaseTargets.create(HELM_DEFAULT_RELEASE_TARGET)
        }
    }
}
