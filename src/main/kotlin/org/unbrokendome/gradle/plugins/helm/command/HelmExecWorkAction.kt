package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.slf4j.LoggerFactory
import org.unbrokendome.gradle.plugins.helm.util.ifPresent
import java.io.OutputStream
import javax.inject.Inject


internal abstract class HelmExecWorkAction
@Inject constructor(
    private val execOperations: ExecOperations
) : WorkAction<HelmExecWorkParameters> {

    private val logger = LoggerFactory.getLogger(javaClass)


    override fun execute() {

        fileOutputStream(parameters.stdoutFile).use { stdout ->
            execOperations.exec { spec ->
                spec.executable = parameters.executable.get()
                spec.args = parameters.args.get()

                val environment = mutableMapOf<String, Any>()

                parameters.environment.ifPresent { environment.putAll(it) }

                // kubectl (and the k8s go client library, which helm uses) depend on the KUBECONFIG environment variable
                // when determining the default kubeconfig location ($HOME/.kube/config). For some reason this isn't
                // inherited from Gradle when the process is launched from a worker, even if no isolation is used.
                environment.computeIfAbsent("KUBECONFIG") {
                    System.getenv("KUBECONFIG")
                }
                // kubectl (and the k8s go client library, which helm uses) depend on the HOME environment variable
                // when determining the default kubeconfig location ($HOME/.kube/config). For some reason this isn't
                // inherited from Gradle when the process is launched from a worker, even if no isolation is used.
                environment.computeIfAbsent("HOME") {
                    System.getenv("HOME") ?: System.getProperty("user.home")
                }
                spec.environment = environment

                stdout?.let { spec.standardOutput = it }

                if (logger.isInfoEnabled) {
                    logger.info("Executing: {}\n  with environment: {}", maskCommandLine(spec.commandLine), environment)
                }
            }
        }
    }


    private fun fileOutputStream(provider: Provider<RegularFile>): OutputStream? =
        provider.orNull?.asFile?.let { file ->
            file.parentFile.mkdirs()
            file.outputStream()
        }
}
