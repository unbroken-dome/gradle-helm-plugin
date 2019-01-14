package org.unbrokendome.gradle.plugins.helm.publishing.dsl

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.unbrokendome.gradle.plugins.helm.publishing.HELM_PUBLISHING_REPOSITORIES_EXTENSION_NAME
import javax.inject.Inject


/**
 * Configures the publishing of Helm charts to remote repositories.
 */
interface HelmPublishingExtension


private open class DefaultHelmPublishingExtension
@Inject constructor()
    : HelmPublishingExtension


/**
 * Creates a new [HelmPublishingExtension] object for the given [Project].
 *
 * @param project the Gradle [Project]
 * @return the created [HelmPublishingExtension] object
 */
internal fun createHelmPublishingExtension(project: Project): HelmPublishingExtension =
        project.objects.newInstance(DefaultHelmPublishingExtension::class.java)
                .apply {
                    (this as ExtensionAware).extensions
                            .add(HELM_PUBLISHING_REPOSITORIES_EXTENSION_NAME,
                                    helmPublishingRepositoryContainer(project))
                }
