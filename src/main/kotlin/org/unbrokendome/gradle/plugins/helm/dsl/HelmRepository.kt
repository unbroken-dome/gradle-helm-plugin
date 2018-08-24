package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.CredentialsContainer
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.CredentialsContainerSupport
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.CredentialsFactory
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.DefaultCredentialsFactory
import org.unbrokendome.gradle.plugins.helm.util.property
import java.net.URI
import javax.inject.Inject


/**
 * Represents a Helm chart repository.
 */
interface HelmRepository : Named, CredentialsContainer {

    /**
     * The URL of this repository.
     */
    val url: Property<URI>
}


private open class DefaultHelmRepository
private constructor(private val name: String,
                    objectFactory: ObjectFactory,
                    credentialsContainer: CredentialsContainer)
    : HelmRepository, CredentialsContainer by credentialsContainer {


    private constructor(name: String, objectFactory: ObjectFactory, credentialsFactory: CredentialsFactory)
            : this(name, objectFactory, CredentialsContainerSupport(credentialsFactory))


    @Inject constructor(name: String, objectFactory: ObjectFactory)
            : this(name, objectFactory, DefaultCredentialsFactory(objectFactory))


    override fun getName(): String =
            name


    override val url: Property<URI> =
            objectFactory.property()
}


/**
 * Creates a [NamedDomainObjectContainer] that holds [HelmRepository] objects.
 *
 * @param project the Gradle [Project]
 * @return the container for `HelmRepository` objects
 */
internal fun helmRepositoryContainer(project: Project): NamedDomainObjectContainer<HelmRepository> =
        project.container(HelmRepository::class.java) { name ->
            project.objects.newInstance(DefaultHelmRepository::class.java, name)
        }
