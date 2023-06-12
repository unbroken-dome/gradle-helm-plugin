package com.citi.gradle.plugins.helm.dsl

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project


/** The name under which the "stable" repository will be registered. */
private const val HELM_STABLE_REPOSITORY_NAME = "stable"

/** URL of the Helm "stable" repository. */
private const val HELM_STABLE_REPOSITORY_URL = "https://charts.helm.sh/stable/"

/** The name under which the "incubator" repository will be registered. */
private const val HELM_INCUBATOR_REPOSITORY_NAME = "incubator"

/** URL of the Helm "incubator" repository. */
private const val HELM_INCUBATOR_REPOSITORY_URL = "https://charts.helm.sh/incubator/"

/** The name under which the Bitnami repository will be registered. */
private const val BITNAMI_REPOSITORY_NAME = "bitnami"

/** URL of the Bitnami repository. */
private const val BITNAMI_REPOSITORY_URL = "https://charts.bitnami.com/bitnami"


interface HelmRepositoryHandler : NamedDomainObjectContainer<HelmRepository> {

    /**
     * Registers the Helm "stable" repository using the name "stable".
     *
     * The sources of the charts in the repository can be found at [https://github.com/helm/charts].
     *
     * @param name the name under which to register the repository, defaults to "stable"
     */
    @Suppress("unused")
    fun helmStable(name: String = HELM_STABLE_REPOSITORY_NAME) {
        register(name) { repo ->
            repo.url(HELM_STABLE_REPOSITORY_URL)
        }
    }


    /**
     * Registers the Helm "incubator" repository using the name "incubator".
     *
     * The sources of the charts in the repository can be found at [https://github.com/helm/charts].
     *
     * @param name the name under which to register the repository, defaults to "incubator"
     */
    @Suppress("unused")
    fun helmIncubator(name: String = HELM_INCUBATOR_REPOSITORY_NAME) {
        register(name) { repo ->
            repo.url(HELM_INCUBATOR_REPOSITORY_URL)
        }
    }


    /**
     * Registers the Bitnami repository.
     *
     * The sources of the charts in the repository can be found at [https://github.com/helm/charts].
     *
     * @param name the name under which to register the repository, defaults to "bitnami"
     */
    @Suppress("unused")
    fun bitnami(name: String = BITNAMI_REPOSITORY_NAME) {
        register(name) { repo ->
            repo.url(BITNAMI_REPOSITORY_URL)
        }
    }
}


private class DefaultHelmRepositoryHandler(
    private val container: NamedDomainObjectContainer<HelmRepository>
) : NamedDomainObjectContainer<HelmRepository> by container, HelmRepositoryHandler



/**
 * Creates a [NamedDomainObjectContainer] that holds [HelmRepository] objects.
 *
 * @receiver the Gradle [Project]
 * @return the container for `HelmRepository` objects
 */
internal fun Project.helmRepositoryHandler(): HelmRepositoryHandler =
    DefaultHelmRepositoryHandler(
        container = helmRepositoryContainer()
    )
