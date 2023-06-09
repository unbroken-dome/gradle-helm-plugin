package com.citi.gradle.plugins.helm.dsl.internal

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import com.citi.gradle.plugins.helm.HELM_CHARTS_EXTENSION_NAME
import com.citi.gradle.plugins.helm.HELM_EXTENSION_NAME
import com.citi.gradle.plugins.helm.HELM_FILTERING_EXTENSION_NAME
import com.citi.gradle.plugins.helm.HELM_LINT_EXTENSION_NAME
import com.citi.gradle.plugins.helm.HELM_REPOSITORIES_EXTENSION_NAME
import com.citi.gradle.plugins.helm.dsl.Filtering
import com.citi.gradle.plugins.helm.dsl.HelmChart
import com.citi.gradle.plugins.helm.dsl.HelmExtension
import com.citi.gradle.plugins.helm.dsl.HelmRepositoryHandler
import com.citi.gradle.plugins.helm.dsl.Linting
import org.unbrokendome.gradle.pluginutils.requiredExtension


/**
 * Gets the [HelmExtension] that is installed on the project.
 */
val Project.helm: HelmExtension
    get() = requiredExtension(HELM_EXTENSION_NAME)


/**
 * Gets the [Linting] sub-extension.
 */
val HelmExtension.lint: Linting
    get() = requiredExtension(HELM_LINT_EXTENSION_NAME)


/**
 * Gets the `repositories` sub-extension.
 */
val HelmExtension.repositories: HelmRepositoryHandler
    get() = requiredExtension(HELM_REPOSITORIES_EXTENSION_NAME)


/**
 * Gets the `charts` sub-extension.
 */
val HelmExtension.charts: NamedDomainObjectContainer<HelmChart>
    get() = requiredExtension(HELM_CHARTS_EXTENSION_NAME)


/**
 * Gets the [Filtering] sub-extension.
 */
val HelmExtension.filtering: Filtering
    get() = requiredExtension(HELM_FILTERING_EXTENSION_NAME)
