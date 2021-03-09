package org.unbrokendome.gradle.plugins.helm.publishing.dsl.internal

import org.unbrokendome.gradle.plugins.helm.dsl.HelmExtension
import org.unbrokendome.gradle.plugins.helm.publishing.HELM_PUBLISHING_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.publishing.HELM_PUBLISHING_REPOSITORIES_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.publishing.dsl.HelmPublishingExtension
import org.unbrokendome.gradle.plugins.helm.publishing.dsl.HelmPublishingRepositoryContainer
import org.unbrokendome.gradle.pluginutils.requiredExtension


/**
 * Gets the `publishing` sub-extension.
 */
val HelmExtension.publishing: HelmPublishingExtension
    get() = requiredExtension(HELM_PUBLISHING_EXTENSION_NAME)


/**
 * Gets the `publishing.repositories` sub-extension.
 */
val HelmPublishingExtension.repositories: HelmPublishingRepositoryContainer
    get() = requiredExtension(HELM_PUBLISHING_REPOSITORIES_EXTENSION_NAME)
