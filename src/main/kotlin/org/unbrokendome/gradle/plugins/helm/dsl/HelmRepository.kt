package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
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

    /**
     * An optional path to a CA bundle used to verify certificates of HTTPS-enabled servers.
     */
    val caFile: RegularFileProperty
}


private open class DefaultHelmRepository
private constructor(
    private val name: String,
    objects: ObjectFactory,
    credentialsContainer: CredentialsContainer
) : HelmRepository, CredentialsContainer by credentialsContainer {


    private constructor(name: String, objects: ObjectFactory, credentialsFactory: CredentialsFactory)
            : this(name, objects, CredentialsContainerSupport(objects, credentialsFactory))


    @Inject
    constructor(name: String, objects: ObjectFactory)
            : this(name, objects, DefaultCredentialsFactory(objects))


    final override fun getName(): String =
        name


    final override val url: Property<URI> =
        objects.property()


    final override val caFile: RegularFileProperty =
        objects.fileProperty()
}


/**
 * Creates a [NamedDomainObjectContainer] that holds [HelmRepository] objects.
 *
 * @receiver the Gradle [Project]
 * @return the container for `HelmRepository` objects
 */
internal fun Project.helmRepositoryContainer(): NamedDomainObjectContainer<HelmRepository> =
    container(HelmRepository::class.java) { name ->
        objects.newInstance(DefaultHelmRepository::class.java, name)
    }
