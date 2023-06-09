package com.citi.gradle.plugins.helm.command

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.workers.WorkerExecutor
import org.json.JSONArray
import org.json.JSONObject
import com.citi.gradle.plugins.helm.command.internal.GlobalHelmOptionsApplier
import com.citi.gradle.plugins.helm.command.internal.HelmOptionsApplier
import com.citi.gradle.plugins.helm.command.internal.HelmServerOptionsApplier
import com.citi.gradle.plugins.helm.command.tasks.AbstractHelmCommandTask
import com.citi.gradle.plugins.helm.model.Release


/**
 * Internal support class for running Helm commands that are not directly mapped by a task.
 */
internal class HelmCommandSupport(
    private val execProvider: HelmExecProviderSupport
) {
    constructor(
        project: Project, workerExecutor: WorkerExecutor, options: HelmOptions,
        optionsApplier: HelmOptionsApplier = GlobalHelmOptionsApplier
    ) : this(HelmExecProviderSupport(project, workerExecutor, options, optionsApplier))


    /**
     * Calls `helm ls` to get the current status of a release.
     *
     * @param releaseName a [Provider] that returns the name of the release
     * @return a [Release] containing information about the release, or `null` if the release does not exist
     */
    fun getRelease(releaseName: Provider<String>): Release? {

        val stdout = execProvider
            .withOptionsAppliers(GlobalHelmOptionsApplier, HelmServerOptionsApplier)
            .withDescription("get release info")
            .execHelmCaptureOutput("ls") { exec ->
                exec.option("-o", "json")
                exec.option("-f", releaseName.map { "^${Regex.escape(it)}$" })
            }

        return JSONArray(stdout)
            .asSequence()
            .map { Release.fromJson(it as JSONObject) }
            .firstOrNull()
    }
}


internal val <T : AbstractHelmCommandTask> T.helmCommandSupport
    get() = HelmCommandSupport(project, workerExecutor, this)
