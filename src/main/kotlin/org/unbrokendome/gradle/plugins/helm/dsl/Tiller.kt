package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.unbrokendome.gradle.plugins.helm.util.booleanProviderFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.intProviderFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.orElse
import org.unbrokendome.gradle.plugins.helm.util.property
import org.unbrokendome.gradle.plugins.helm.util.providerFromProjectProperty
import javax.inject.Inject


/**
 * Defines options for setting up Tiller on a remote Kubernetes cluster.
 */
interface Tiller {

    /**
     * Whether to install Tiller on the cluster.
     */
    val install: Property<Boolean>


    /**
     * Namespace of Tiller (default "kube-system")
     */
    val namespace: Property<String>


    /**
     * If `true`, force upgrade of Tiller to the current Helm version.
     */
    val forceUpgrade: Property<Boolean>


    /**
     * Limit the maximum number of revisions saved per release. Use `0` for no limit.
     */
    val historyMax: Property<Int>


    /**
     * Amount of Tiller instances to run on the cluster.
     */
    val replicas: Property<Int>


    /**
     * Name of service account.
     */
    val serviceAccount: Property<String>


    /**
     * Override Tiller image.
     */
    val image: Property<String>


    /**
     * Upgrade if Tiller is already installed.
     */
    val upgrade: Property<Boolean>


    /**
     * If `true`, block until Tiller is running and ready to receive requests.
     */
    val wait: Property<Boolean>
}


private open class DefaultTiller
@Inject constructor(project: Project)
    : Tiller {


    override val install: Property<Boolean> =
            project.objects.property(
                    project.booleanProviderFromProjectProperty("helm.tiller.install")
                            .orElse(true))


    override val namespace: Property<String> =
            project.objects.property(
                    project.providerFromProjectProperty("helm.tiller.namespace"))


    override val forceUpgrade: Property<Boolean> =
            project.objects.property(
                    project.booleanProviderFromProjectProperty("helm.tiller.forceUpgrade"))


    override val historyMax: Property<Int> =
            project.objects.property(
                    project.intProviderFromProjectProperty("helm.tiller.historyMax"))


    override val replicas: Property<Int> =
            project.objects.property(
                    project.intProviderFromProjectProperty("helm.tiller.replicas"))


    override val serviceAccount: Property<String> =
            project.objects.property(
                    project.providerFromProjectProperty("helm.tiller.serviceAccount"))


    override val image: Property<String> =
            project.objects.property(
                    project.providerFromProjectProperty("helm.tiller.image"))


    override val upgrade: Property<Boolean> =
            project.objects.property(
                    project.booleanProviderFromProjectProperty("helm.tiller.upgrade"))


    override val wait: Property<Boolean> =
            project.objects.property(
                    project.booleanProviderFromProjectProperty("helm.tiller.wait")
                            .orElse(true))
}


/**
 * Creates a new [Tiller] object using the given project's [ObjectFactory].
 *
 * @param project the Gradle [Project]
 * @return the created [Tiller] object
 */
internal fun createTiller(project: Project): Tiller =
        project.objects.newInstance(DefaultTiller::class.java, project)
