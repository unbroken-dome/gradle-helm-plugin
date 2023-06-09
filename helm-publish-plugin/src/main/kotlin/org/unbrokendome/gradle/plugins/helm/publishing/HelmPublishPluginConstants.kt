package com.citi.gradle.plugins.helm.publishing


/**
 * The name of the `publishing` sub-extension.
 */
internal const val HELM_PUBLISHING_EXTENSION_NAME = "publishing"


/**
 * The name of the `publishing.repositories` sub-extension.
 */
internal const val HELM_PUBLISHING_REPOSITORIES_EXTENSION_NAME = "repositories"


/**
 * The name of the publishing convention object installed on each chart.
 *
 * Note that since convention properties are accessible directly on the object, the name of the convention
 * does not really matter except for uniqueness.
 */
const val HELM_CHART_PUBLISHING_CONVENTION_NAME = "publishing"
