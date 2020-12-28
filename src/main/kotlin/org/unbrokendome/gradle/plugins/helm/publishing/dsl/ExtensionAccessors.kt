package org.unbrokendome.gradle.plugins.helm.publishing.dsl

import org.unbrokendome.gradle.plugins.helm.dsl.HelmExtension
import org.unbrokendome.gradle.plugins.helm.publishing.HELM_PUBLISHING_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.publishing.HELM_PUBLISHING_REPOSITORIES_EXTENSION_NAME
import org.unbrokendome.gradle.pluginutils.extensionByName


/**
 * Gets the `publishing` sub-extension.
 */
internal val HelmExtension.publishing: HelmPublishingExtension
        by extensionByName(HELM_PUBLISHING_EXTENSION_NAME)


/**
 * Gets the `publishing.repositories` sub-extension.
 */
internal val HelmPublishingExtension.repositories: HelmPublishingRepositoryContainer
        by extensionByName(HELM_PUBLISHING_REPOSITORIES_EXTENSION_NAME)
