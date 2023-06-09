package com.citi.gradle.plugins.helm.dsl

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import com.citi.gradle.plugins.helm.dsl.credentials.CredentialsContainer
import com.citi.gradle.plugins.helm.dsl.credentials.internal.CredentialsContainerSupport
import com.citi.gradle.plugins.helm.dsl.credentials.internal.CredentialsFactory
import com.citi.gradle.plugins.helm.dsl.credentials.internal.DefaultCredentialsFactory
import org.unbrokendome.gradle.pluginutils.property
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
     * Sets the URL of the repository to the given value.
     *
     * This is a shortcut for calling
     * ```
     * url.set( project.uri(path) )
     * ```
     *
     * @param path the URL or path, evaluated as per [Project.uri]
     */
    fun url(path: Any)


    /**
     * An optional path to a CA bundle used to verify certificates of HTTPS-enabled servers.
     */
    val caFile: RegularFileProperty
}


private open class DefaultHelmRepository
private constructor(
    private val project: Project,
    private val name: String,
    credentialsContainer: CredentialsContainer
) : HelmRepository, CredentialsContainer by credentialsContainer {


    private constructor(project: Project, name: String, credentialsFactory: CredentialsFactory)
            : this(project, name, CredentialsContainerSupport(project.objects, credentialsFactory))


    @Inject
    constructor(project: Project, name: String)
            : this(project, name, DefaultCredentialsFactory(project.objects))


    final override fun getName(): String =
        name


    final override val url: Property<URI> =
        project.objects.property()


    override fun url(path: Any) {
        this.url.set(project.uri(path))
    }


    final override val caFile: RegularFileProperty =
        project.objects.fileProperty()
}


/**
 * Creates a [NamedDomainObjectContainer] that holds [HelmRepository] objects.
 *
 * @receiver the Gradle [Project]
 * @return the container for `HelmRepository` objects
 */
internal fun Project.helmRepositoryContainer(): NamedDomainObjectContainer<HelmRepository> =
    container(HelmRepository::class.java) { name ->
        objects.newInstance(DefaultHelmRepository::class.java, project, name)
    }
