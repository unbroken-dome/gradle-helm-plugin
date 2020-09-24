package org.unbrokendome.gradle.plugins.helm.publishing.dsl

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.CredentialsContainer
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.CredentialsContainerSupport
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.CredentialsFactory
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.DefaultCredentialsFactory


internal abstract class AbstractHelmPublishingRepository
private constructor(
    private val name: String,
    credentialsContainer: CredentialsContainer
) : HelmPublishingRepositoryInternal, CredentialsContainer by credentialsContainer {

    private constructor(objects: ObjectFactory, name: String, credentialsFactory: CredentialsFactory)
            : this(name, CredentialsContainerSupport(objects, credentialsFactory))


    protected constructor(objects: ObjectFactory, name: String)
            : this(objects, name, DefaultCredentialsFactory(objects))


    protected constructor(project: Project, name: String)
            : this(project.objects, name)


    final override fun getName(): String =
        name
}
