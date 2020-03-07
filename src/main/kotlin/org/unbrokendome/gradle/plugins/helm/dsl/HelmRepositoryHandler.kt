package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import java.net.URI


interface HelmRepositoryHandler : NamedDomainObjectContainer<HelmRepository> {

    companion object {

        /**
         * The name under which the "stable" repository will be registered.
         */
        @JvmStatic
        val HELM_STABLE_REPOSITORY_NAME = "stable"

        /**
         * URL of the Helm "stable" repository.
         */
        @JvmStatic
        val HELM_STABLE_REPOSITORY_URL = URI("https://kubernetes-charts.storage.googleapis.com/")

        /**
         * The name under which the "incubator" repository will be registered.
         */
        @JvmStatic
        val HELM_INCUBATOR_REPOSITORY_NAME = "incubator"

        /**
         * URL of the Helm "incubator" repository.
         */
        @JvmStatic
        val HELM_INCUBATOR_REPOSITORY_URL = URI("https://kubernetes-charts-incubator.storage.googleapis.com/")
    }


    /**
     * Registers the Helm "stable" repository using the name "stable".
     *
     * The sources of the charts in the repository can be found at [https://github.com/helm/charts].
     */
    @JvmDefault
    fun helmStable(): HelmRepository =
        create(HELM_STABLE_REPOSITORY_NAME) { repo ->
            repo.url.set(HELM_STABLE_REPOSITORY_URL)
        }


    /**
     * Registers the Helm "incubator" repository using the name "incubator".
     *
     * The sources of the charts in the repository can be found at [https://github.com/helm/charts].
     */
    @JvmDefault
    fun helmIncubator(): HelmRepository =
        create(HELM_INCUBATOR_REPOSITORY_NAME) { repo ->
            repo.url.set(HELM_INCUBATOR_REPOSITORY_URL)
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
