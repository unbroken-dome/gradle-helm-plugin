package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.slf4j.LoggerFactory
import org.unbrokendome.gradle.pluginutils.ifPresent
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

                parameters.environment.ifPresent { spec.environment.putAll(it) }

                spec.standardOutput = stdout ?: LoggerOutputStream { message -> logger.info(message) }
                spec.errorOutput = stdout ?: LoggerOutputStream { message -> logger.error(message) }

                if (logger.isInfoEnabled) {
                    logger.info("Executing: {}\n  with environment: {}", maskCommandLine(spec.commandLine), spec.environment)
                }
            }
        }
    }

    private fun fileOutputStream(provider: Provider<RegularFile>): OutputStream? =
        provider.orNull?.asFile?.let { file ->
            file.parentFile.mkdirs()
            file.outputStream()
        }

    class LoggerOutputStream(val log: (String) -> Unit) : OutputStream() {
        private val currentLine: StringBuilder = StringBuilder()

        override fun write(b: Int) {
            if (b.toChar() == '\n') {
                flush()
                return
            }
            currentLine.append(b.toChar())
        }

        override fun flush() {
            log(currentLine.toString())
            currentLine.clear()
        }
  }
}
