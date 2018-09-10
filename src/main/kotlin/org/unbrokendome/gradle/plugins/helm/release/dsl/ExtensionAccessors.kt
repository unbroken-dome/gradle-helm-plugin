package org.unbrokendome.gradle.plugins.helm.release.dsl

import org.gradle.api.NamedDomainObjectContainer
import org.unbrokendome.gradle.plugins.helm.HELM_RELEASES_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.dsl.HelmExtension
import org.unbrokendome.gradle.plugins.helm.util.requiredExtension


/**
 * Gets the `releases` sub-extension.
 */
val HelmExtension.releases: NamedDomainObjectContainer<HelmRelease>
    get() = requiredExtension(HELM_RELEASES_EXTENSION_NAME)
