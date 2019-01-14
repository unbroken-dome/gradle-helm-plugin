package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.unbrokendome.gradle.plugins.helm.util.property


/**
 * Initialize Helm on the client and/or server side. Corresponds to the `helm init` CLI command.
 */
open class HelmInit : AbstractHelmCommandTask() {

    /**
     * If `true`, does not install Tiller.
     */
    @get:Input
    val clientOnly: Property<Boolean> =
            project.objects.property<Boolean>()
                    .convention(false)


    /**
     * If `true`, force upgrade of Tiller to the current Helm version.
     */
    @get:[Input Optional]
    val forceUpgrade: Property<Boolean> =
            project.objects.property()


    /**
     * Limit the maximum number of revisions saved per release. Use `0` for no limit.
     */
    @get:[Input Optional]
    val historyMax: Property<Int> =
            project.objects.property()


    /**
     * Amount of Tiller instances to run on the cluster.
     */
    @get:[Input Optional]
    val replicas: Property<Int> =
            project.objects.property()


    /**
     * Name of service account.
     */
    @get:[Input Optional]
    val serviceAccount: Property<String> =
            project.objects.property()


    /**
     * If `true`, do not refresh (download) the local repository cache.
     */
    @get:[Input Optional]
    val skipRefresh: Property<Boolean> =
            project.objects.property()


    /**
     * Override Tiller image.
     */
    @get:[Input Optional]
    val tillerImage: Property<String> =
            project.objects.property()


    /**
     * Upgrade if Tiller is already installed.
     */
    @get:[Input Optional]
    val upgrade: Property<Boolean> =
            project.objects.property()


    /**
     * If `true`, block until Tiller is running and ready to receive requests.
     */
    @get:Internal
    val wait: Property<Boolean> =
            project.objects.property()


    init {
        outputs.upToDateWhen { clientOnly.get() }
        outputs.upToDateWhen { home.orNull?.asFile?.isDirectory ?: true }
    }


    @TaskAction
    fun helmInit() {
        execHelm("init") {
            flag("--client-only", clientOnly)
            flag("--force-upgrade", forceUpgrade)
            option("--history-max", historyMax)
            option("--replicas", replicas)
            option("--service-account", serviceAccount)
            flag("--skip-refresh", skipRefresh)
            option("--tiller-image", tillerImage)
        }
    }
}
