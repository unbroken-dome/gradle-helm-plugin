package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.unbrokendome.gradle.plugins.helm.util.booleanProviderFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.intProviderFromProjectProperty
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
@Inject constructor(project: Project) : Tiller {


    final override val install: Property<Boolean> =
        project.objects.property<Boolean>()
            .convention(project.booleanProviderFromProjectProperty("helm.tiller.install", defaultValue = true))


    final override val namespace: Property<String> =
        project.objects.property<String>()
            .convention(project.providerFromProjectProperty("helm.tiller.namespace"))


    final override val forceUpgrade: Property<Boolean> =
        project.objects.property<Boolean>()
            .convention(project.booleanProviderFromProjectProperty("helm.tiller.forceUpgrade"))


    final override val historyMax: Property<Int> =
        project.objects.property<Int>()
            .convention(project.intProviderFromProjectProperty("helm.tiller.historyMax"))


    final override val replicas: Property<Int> =
        project.objects.property<Int>()
            .convention(project.intProviderFromProjectProperty("helm.tiller.replicas"))


    final override val serviceAccount: Property<String> =
        project.objects.property<String>()
            .convention(project.providerFromProjectProperty("helm.tiller.serviceAccount"))


    final override val image: Property<String> =
        project.objects.property<String>()
            .convention(project.providerFromProjectProperty("helm.tiller.image"))


    final override val upgrade: Property<Boolean> =
        project.objects.property<Boolean>()
            .convention(project.booleanProviderFromProjectProperty("helm.tiller.upgrade"))


    final override val wait: Property<Boolean> =
        project.objects.property<Boolean>()
            .convention(project.booleanProviderFromProjectProperty("helm.tiller.wait", defaultValue = true))
}


/**
 * Creates a new [Tiller] object using the given project's [ObjectFactory].
 *
 * @receiver the Gradle [Project]
 * @return the created [Tiller] object
 */
internal fun Project.createTiller(): Tiller =
    objects.newInstance(DefaultTiller::class.java, this)
