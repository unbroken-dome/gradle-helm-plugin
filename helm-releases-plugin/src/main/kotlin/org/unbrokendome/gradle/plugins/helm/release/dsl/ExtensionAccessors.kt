package org.unbrokendome.gradle.plugins.helm.release.dsl

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.Property
import org.unbrokendome.gradle.plugins.helm.HELM_ACTIVE_RELEASE_TARGET_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.HELM_RELEASES_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.HELM_RELEASE_TARGETS_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.dsl.HelmExtension
import org.unbrokendome.gradle.pluginutils.extensionByName


/**
 * Gets the `releases` sub-extension.
 */
internal val HelmExtension.releases: NamedDomainObjectContainer<HelmRelease>
        by extensionByName(HELM_RELEASES_EXTENSION_NAME)


/**
 * Gets the `releaseTargets` sub-extension.
 */
internal val HelmExtension.releaseTargets: NamedDomainObjectContainer<HelmReleaseTarget>
        by extensionByName(HELM_RELEASE_TARGETS_EXTENSION_NAME)


/**
 * Gets the `activeReleaseTarget` sub-extension.
 */
internal val HelmExtension.activeReleaseTarget: Property<String>
        by extensionByName(HELM_ACTIVE_RELEASE_TARGET_EXTENSION_NAME)
