package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.json.JSONArray
import org.json.JSONObject
import org.unbrokendome.gradle.plugins.helm.model.Release


/**
 * Internal support class for running Helm commands that are not directly mapped by a task.
 */
internal class HelmCommandSupport(
    private val execProvider: HelmExecProviderSupport
) {
    constructor(
        project: Project, options: HelmOptions, optionsApplier: HelmOptionsApplier = GlobalHelmOptionsApplier
    ) : this(HelmExecProviderSupport(project, options, optionsApplier))


    /**
     * Calls `helm ls` to get the current status of a release.
     *
     * @param releaseName a [Provider] that returns the name of the release
     * @return a [Release] containing information about the release, or `null` if the release does not exist
     */
    fun getRelease(releaseName: Provider<String>): Release? {

        val result = execProvider
            .withOptionsAppliers(GlobalHelmOptionsApplier, HelmServerOptionsApplier)
            .withDescription("get release info")
            .execHelmCaptureOutput("ls") {
                option("-o", "json")
                option("-f", releaseName.map { "^${Regex.escape(it)}$" })
            }

        return JSONArray(result.stdout)
            .asSequence()
            .map { Release.fromJson(it as JSONObject) }
            .firstOrNull()
    }
}


internal val <T> T.helmCommandSupport
        where T : Task, T : HelmOptions
    get() = HelmCommandSupport(project, this)
