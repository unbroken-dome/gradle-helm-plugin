package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.unbrokendome.gradle.plugins.helm.util.ifPresent


/**
 * Configures and executes a Helm CLI command.
 */
interface HelmRunner {

    /**
     * Adds some arguments to the CLI invocation.
     *
     * @param args the command line argument(s) to be added
     */
    fun args(vararg args: Any)

    /**
     * Adds arguments supplied by a Gradle [Provider] to the CLI invocation.
     *
     * The provider is evaluated when the process is invoked, and if the provider has a value then
     * it is added as a command-line argument.
     *
     * If the provider has a [Collection]-typed value then each item of the collection is added as
     * a separate argument.
     *
     * @param provider the provider of the command line argument(s) to be added
     */
    fun args(provider: Provider<out Any>)

    /**
     * Adds a flag argument to the command line.
     *
     * Flags are arguments that are either `true` or `false` (set or unset). Unless documented
     * otherwise in the Helm CLI, most flags are unset by default and must be explicitly set.
     *
     * A flag will only be added to the command line if its value differs from the default value.
     * A flag that is set will be added as `--flag` to the command line, while a flag that is
     * unset will be added as `--flag=false`.
     *
     * @param name the name of the flag, including the leading dashes (e.g. `--debug`)
     * @param value whether the flag should be set or unset
     * @param defaultValue whether the flag is considered set or unset by default
     */
    fun flag(name: String, value: Boolean = true, defaultValue: Boolean = false)

    /**
     * Adds a flag argument to the command line, using a [Provider] to supply the flag value.
     *
     * Flags are arguments that are either `true` or `false` (set or unset). Unless documented
     * otherwise in the Helm CLI, most flags are unset by default and must be explicitly set.
     *
     * The provider is evaluated when the process is invoked, and its value is used to set/unset
     * the flag. If the provider doesn't have a value then no argument will be added to the
     * command line.
     *
     * A flag will only be added to the command line if its value differs from the default value.
     * A flag that is set will be added as `--flag` to the command line, while a flag that is
     * unset will be added as `--flag=false`.
     *
     * @param name the name of the flag, including the leading dashes (e.g. `--debug`)
     * @param provider provider of the flag value
     * @param defaultValue whether the flag is considered set or unset by default
     */
    fun flag(name: String, provider: Provider<Boolean>, defaultValue: Boolean = false)

    /**
     * Adds an option argument to the command line, using the given value.
     *
     * Options are arguments that have a value, like `--foo=bar`.
     *
     * @param name the name of the option, including the leading dashes (e.g. `--home`)
     * @param value the value of the option
     */
    fun option(name: String, value: Any)

    /**
     * Adds an option argument to the command line, using a [Provider] to supply the option value.
     *
     * Options are arguments that have a value, like `--foo=bar`.
     *
     * The provider is evaluated when the process is invoked, and its value used for the option.
     * If the provider does not have a value, then the option will not be added at all.
     *
     * @param name the name of the option, including the leading dashes (e.g. `--home`)
     * @param provider the value of the option
     */
    fun option(name: String, provider: Provider<out Any>)

    /**
     * Sets an environment variable for the process, using a [Provider] to supply the variable value.
     *
     * If the provider does not have a value, the environment variable will not be set.
     *
     * @param name the name of the environment variable
     * @param provider the provider supplying the value of the environment variable
     */
    fun environment(name: String, provider: Provider<out Any>)

    /**
     * If true (the default), the [run] method will fail with an exception if the process returns
     * a non-zero exit code.
     */
    fun assertSuccess(assertSuccess: Boolean = true)

    /**
     * Invokes the Helm command with the current configuration.
     *
     * @return an [ExecResult] indicating the result of the invocation
     */
    fun run(): ExecResult
}


/**
 * Default implementation of [HelmRunner].
 *
 * Uses Gradle's [Project.exec] to invoke the Helm CLI.
 */
internal class DefaultHelmRunner(
        private val execFn: ((ExecSpec) -> Unit) -> ExecResult,
        private val globalOptions: GlobalHelmOptions,
        private val command: String,
        private val subcommand: String?)
    : HelmRunner {

    constructor(project: Project,
                globalOptions: GlobalHelmOptions,
                command: String,
                subcommand: String?)
    : this({ project.exec(it) }, globalOptions, command, subcommand)


    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val actions = mutableListOf<Action<ExecSpec>>()
    private var assertSuccess: Boolean = true

    init {
        environment("HELM_HOME", globalOptions.home)
        flag("--debug", globalOptions.debug)
    }


    override fun args(vararg args: Any) {
        actions.add(Action { it.args(*args) })
    }


    override fun args(provider: Provider<out Any>) {
        actions.add(Action { spec ->
            provider.ifPresent { value ->
                if (value is Collection<*>) {
                    spec.args(*value.toTypedArray())
                } else {
                    spec.args(value)
                }
            }
        })
    }


    override fun flag(name: String, value: Boolean, defaultValue: Boolean) {
        if (value != defaultValue) {
            actions.add(Action { spec ->
                spec.args(if (value) name else "$name=false")
            })
        }
    }


    override fun flag(name: String, provider: Provider<Boolean>, defaultValue: Boolean) {
        actions.add(Action { spec ->
            provider.orNull
                    ?.takeIf { it != defaultValue }
                    ?.let { value ->
                        spec.args(if (value) name else "$name=false")
                    }
        })
    }


    override fun option(name: String, value: Any) {
        actions.add(Action { spec ->
            spec.args(name, value)
        })
    }


    override fun option(name: String, provider: Provider<out Any>) {
        actions.add(Action { spec ->
            provider.ifPresent { value ->
                spec.args(name, value)
            }
        })
    }


    override fun environment(name: String, provider: Provider<out Any>) {
        actions.add(Action { spec ->
            provider.ifPresent { value ->
                spec.environment(name, value)
            }
        })
    }


    override fun assertSuccess(assertSuccess: Boolean) {
        this.assertSuccess = assertSuccess
    }


    private fun apply(spec: ExecSpec) {
        spec.executable = globalOptions.executable.getOrElse("helm")
        spec.isIgnoreExitValue = !assertSuccess

        spec.args(command)
        subcommand?.let { spec.args(it) }

        actions.forEach { it.execute(spec) }

        globalOptions.extraArgs.ifPresent { extraArgs ->
            spec.args(extraArgs)
        }
    }


    override fun run(): ExecResult =
        execFn { spec ->
            apply(spec)
            if (logger.isInfoEnabled) {
                logger.info("Executing: {}", maskCommandLine(spec.commandLine))
            }
        }


    private fun maskCommandLine(commandLine: List<String>): List<String> =
            commandLine.mapIndexed { index, arg ->
                if (index > 0 && shouldMaskOptionValue(commandLine[index - 1])) "******" else arg
            }


    private fun shouldMaskOptionValue(arg: String) =
            arg.startsWith("--") && arg.contains("password")
}
