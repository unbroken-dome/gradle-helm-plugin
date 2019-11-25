package org.unbrokendome.gradle.plugins.helm.command.tasks

import com.vdurmont.semver4j.Semver
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecResult
import java.io.ByteArrayOutputStream


/**
 * Get the helm version client and/or server side. Corresponds to the `helm version` CLI command.
 */
open class HelmVersion : AbstractHelmCommandTask() {
    internal companion object {
        const val clientPrefix2 = "Client: &version.Version"
        const val serverPrefix2 = "Server: &version.Version"
        const val clientPrefix3 = "version.BuildInfo"
    }

    init {
        outputs.upToDateWhen { false }
    }

    @get:[Internal]
    var clientVersion: Semver = Semver("0.0.0")

    @get:[Internal]
    var serverVersion: Semver = Semver("0.0.0")

    @TaskAction
    fun helmVersion() {
        //2.0.0 output
        //Client: &version.Version{SemVer:"v2.14.3", GitCommit:"0e7f3b6637f7af8fcfddb3d2941fcc7cbebb0085", GitTreeState:"clean"}
        //Server: &version.Version{SemVer:"v2.14.3", GitCommit:"0e7f3b6637f7af8fcfddb3d2941fcc7cbebb0085", GitTreeState:"clean"}

        //3.0.0 output
        //version.BuildInfo{Version:"v3.0.0", GitCommit:"e29ce2a54e96cd02ccfce88bee4f58bb6e2a28b6", GitTreeState:"clean", GoVersion:"go1.13.4"}

        val stdOut = ByteArrayOutputStream()
        val execResult: ExecResult = execHelm("version") {
            withExecSpec {
                standardOutput = stdOut
                setIgnoreExitValue(true)
            }
        }
        if (execResult.exitValue == 0) {
            val outputLines: List<String> = stdOut.toString().trim().lines()
            outputLines.forEach {
                if (it.startsWith(clientPrefix3)) {
                    //helm 3.x clent prefix
                    var version: String? = "Version\\:\\\"v(\\d+\\.\\d+\\.\\d+)\\\"".toRegex().find(it)?.groupValues?.get(1)
                    if (version != null) {
                        clientVersion = Semver(version)
                    }
                } else if (it.startsWith(clientPrefix2)) {
                    //helm 2.x server prefix
                    var version: String? = "SemVer\\:\\\"v(\\d+\\.\\d+\\.\\d+)\\\"".toRegex().find(it)?.groupValues?.get(1)
                    if (version != null) {
                        clientVersion = Semver(version)
                    }
                } else if (it.startsWith(serverPrefix2)) {
                    //helm 2.x server prefix
                    var version: String? = "SemVer\\:\\\"v(\\d+\\.\\d+\\.\\d+)\\\"".toRegex().find(it)?.groupValues?.get(1)
                    if (version != null) {
                        serverVersion = Semver(version)
                    }
                }
            }
            logger.info("Client: ${clientVersion}")
            logger.info("Server: ${serverVersion}")
        }
    }
}
