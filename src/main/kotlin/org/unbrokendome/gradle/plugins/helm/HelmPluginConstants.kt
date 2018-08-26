@file:JvmName("HelmPluginConstants")
package org.unbrokendome.gradle.plugins.helm


/**
 * The task group for all Helm-related tasks.
 */
const val HELM_GROUP = "helm"


/**
 * The name of the `helm` extension.
 */
const val HELM_EXTENSION_NAME = "helm"


/**
 * The name of the `lint` DSL extension.
 */
const val HELM_LINT_EXTENSION_NAME = "lint"


/**
 * The name of the `repositories` DSL sub-extension.
 */
const val HELM_REPOSITORIES_EXTENSION_NAME = "repositories"


/**
 * The name of the `charts` DSL sub-extension.
 */
const val HELM_CHARTS_EXTENSION_NAME = "charts"


/**
 * The name of the `filtering` DSL extension.
 */
const val HELM_FILTERING_EXTENSION_NAME = "filtering"


/**
 * The name of the `dependencies` DSL extension on a chart.
 */
const val HELM_DEPENDENCIES_EXTENSION_NAME = "dependencies"


/**
 * The name of the special "main" chart.
 */
const val HELM_MAIN_CHART_NAME = "main"
