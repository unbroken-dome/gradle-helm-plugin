package org.unbrokendome.gradle.plugins.helm.publishing.dsl

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.CredentialsContainer
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.CredentialsContainerSupport
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.CredentialsFactory
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.DefaultCredentialsFactory
import org.unbrokendome.gradle.plugins.helm.publishing.HttpHelmChartPublisher
import org.unbrokendome.gradle.plugins.helm.publishing.HelmChartPublisher
import org.unbrokendome.gradle.plugins.helm.util.property
import java.net.URI
import javax.inject.Inject


/**
 * Describes a remote repository into which Helm charts can be published.
 */
interface HelmPublishingRepository : Named, CredentialsContainer {

    /**
     * The URL of the repository.
     */
    @get:Input
    val url: Property<URI>

    /**
     * The HTTP method used to upload the chart packages. Defaults to `POST`.
     */
    @get:Input
    val uploadMethod: Property<String>

    /**
     * The path, relative to the base [url], with the API endpoint for publishing charts.
     * Defaults to `"/api/charts"`.
     */
    @get:Input
    val uploadPath: Property<String>
}


internal interface HelmPublishingRepositoryInternal : HelmPublishingRepository {

    /**
     * Gets the [HelmChartPublisher] that can be used to publish a chart into this repository.
     */
    val publisher: HelmChartPublisher
}


private open class DefaultHelmPublishingRepository
private constructor(private val name: String,
                    objectFactory: ObjectFactory,
                    credentialsContainer: CredentialsContainer)
    : HelmPublishingRepositoryInternal, CredentialsContainer by credentialsContainer {


    private constructor(name: String, objectFactory: ObjectFactory, credentialsFactory: CredentialsFactory)
            : this(name, objectFactory, CredentialsContainerSupport(credentialsFactory))


    @Inject constructor(name: String, objectFactory: ObjectFactory)
            : this(name, objectFactory, DefaultCredentialsFactory(objectFactory))


    override fun getName(): String =
            name


    override val url: Property<URI> =
            objectFactory.property()


    override val uploadMethod: Property<String> =
            objectFactory.property("POST")


    override val uploadPath: Property<String> =
            objectFactory.property("/api/charts")


    override val publisher: HelmChartPublisher
        get() = HttpHelmChartPublisher(url.get(), uploadMethod.get(), uploadPath.orNull, configuredCredentials)
}


/**
 * Creates a [NamedDomainObjectContainer] that holds [HelmPublishingRepository] objects.
 *
 * @param project the Gradle [Project]
 * @return the container for `HelmPublishingRepository` objects
 */
internal fun helmPublishingRepositoryContainer(project: Project): NamedDomainObjectContainer<HelmPublishingRepository> =
        project.container(HelmPublishingRepository::class.java) { name ->
            project.objects.newInstance(DefaultHelmPublishingRepository::class.java, name)
        }
