package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.unbrokendome.gradle.plugins.helm.HELM_CHARTS_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.HELM_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.HELM_FILTERING_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.HELM_LINT_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.HELM_REPOSITORIES_EXTENSION_NAME
import org.unbrokendome.gradle.pluginutils.extensionByName


/**
 * Gets the [HelmExtension] that is installed on the project.
 */
internal val Project.helm: HelmExtension
        by extensionByName(HELM_EXTENSION_NAME)


/**
 * Gets the [Linting] sub-extension.
 */
internal val HelmExtension.lint: Linting
        by extensionByName(HELM_LINT_EXTENSION_NAME)


/**
 * Gets the `repositories` sub-extension.
 */
internal val HelmExtension.repositories: HelmRepositoryHandler
        by extensionByName(HELM_REPOSITORIES_EXTENSION_NAME)


/**
 * Gets the `charts` sub-extension.
 */
internal val HelmExtension.charts: NamedDomainObjectContainer<HelmChart>
        by extensionByName(HELM_CHARTS_EXTENSION_NAME)


/**
 * Gets the [Filtering] sub-extension.
 */
internal val HelmExtension.filtering: Filtering
        by extensionByName(HELM_FILTERING_EXTENSION_NAME)
