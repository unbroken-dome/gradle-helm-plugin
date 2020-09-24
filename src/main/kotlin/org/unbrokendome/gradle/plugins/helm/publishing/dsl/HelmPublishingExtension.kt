package org.unbrokendome.gradle.plugins.helm.publishing.dsl

import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.internal.reflect.Instantiator
import org.unbrokendome.gradle.plugins.helm.publishing.HELM_PUBLISHING_REPOSITORIES_EXTENSION_NAME


/**
 * Configures the publishing of Helm charts to remote repositories.
 */
interface HelmPublishingExtension


/**
 * Creates a new [HelmPublishingExtension].
 *
 * @receiver the Gradle [ObjectFactory]
 * @return the created [HelmPublishingExtension] object
 */
internal fun ObjectFactory.createHelmPublishingExtension(instantiator: Instantiator): HelmPublishingExtension =
    newInstance(HelmPublishingExtension::class.java)
        .apply {
            (this as ExtensionAware).extensions
                .add(
                    HelmPublishingRepositoryContainer::class.java,
                    HELM_PUBLISHING_REPOSITORIES_EXTENSION_NAME,
                    newHelmPublishingRepositoryContainer(instantiator)
                )
        }
