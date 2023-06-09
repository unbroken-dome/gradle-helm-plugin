package com.citi.gradle.plugins.helm.publishing.dsl

import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.internal.reflect.Instantiator
import com.citi.gradle.plugins.helm.publishing.HELM_PUBLISHING_REPOSITORIES_EXTENSION_NAME
import javax.inject.Inject


/**
 * Configures the publishing of Helm charts to remote repositories.
 */
interface HelmPublishingExtension


private open class DefaultHelmPublishingExtension
@Inject constructor() : HelmPublishingExtension


/**
 * Creates a new [HelmPublishingExtension].
 *
 * @receiver the Gradle [ObjectFactory]
 * @return the created [HelmPublishingExtension] object
 */
internal fun ObjectFactory.createHelmPublishingExtension(instantiator: Instantiator): HelmPublishingExtension =
    newInstance(DefaultHelmPublishingExtension::class.java)
        .apply {
            (this as ExtensionAware).extensions
                .add(
                    HelmPublishingRepositoryContainer::class.java,
                    HELM_PUBLISHING_REPOSITORIES_EXTENSION_NAME,
                    newHelmPublishingRepositoryContainer(instantiator)
                )
        }
