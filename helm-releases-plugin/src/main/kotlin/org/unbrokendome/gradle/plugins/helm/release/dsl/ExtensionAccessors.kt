package com.citi.gradle.plugins.helm.release.dsl

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.Property
import com.citi.gradle.plugins.helm.HELM_ACTIVE_RELEASE_TARGET_EXTENSION_NAME
import com.citi.gradle.plugins.helm.HELM_RELEASES_EXTENSION_NAME
import com.citi.gradle.plugins.helm.HELM_RELEASE_TARGETS_EXTENSION_NAME
import com.citi.gradle.plugins.helm.dsl.HelmExtension
import org.unbrokendome.gradle.pluginutils.requiredExtension


/**
 * Gets the `releases` sub-extension.
 */
internal val HelmExtension.releases: NamedDomainObjectContainer<HelmRelease>
    get() = requiredExtension(HELM_RELEASES_EXTENSION_NAME)


/**
 * Gets the `releaseTargets` sub-extension.
 */
internal val HelmExtension.releaseTargets: NamedDomainObjectContainer<HelmReleaseTarget>
    get() = requiredExtension(HELM_RELEASE_TARGETS_EXTENSION_NAME)


/**
 * Gets the `activeReleaseTarget` sub-extension.
 */
@Suppress("UNCHECKED_CAST")
internal val HelmExtension.activeReleaseTarget: Property<String>
    get() = requiredExtension(HELM_ACTIVE_RELEASE_TARGET_EXTENSION_NAME)
