package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.process.ExecSpec
import org.unbrokendome.gradle.plugins.helm.util.ifPresent


/**
 * Configures and executes a Helm CLI command.
 */
interface HelmExecSpec {

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
     * If true (the default), executing the command will fail with an exception if the process returns
     * a non-zero exit code.
     */
    fun assertSuccess(assertSuccess: Boolean = true)

    /**
     * Adds an action that directly manipulates the underlying [ExecSpec].
     *
     * @param action the action to execute on the [ExecSpec]
     */
    fun withExecSpec(action: Action<ExecSpec>)

    /**
     * Adds an action that directly manipulates the underlying [ExecSpec].
     *
     * @param action the action to execute on the [ExecSpec]
     */
    @JvmDefault
    fun withExecSpec(action: ExecSpec.() -> Unit) =
            withExecSpec(Action(action))
}


/**
 * Default implementation of [HelmExecSpec].
 *
 * Uses Gradle's [Project.exec] to invoke the Helm CLI.
 */
internal class DefaultHelmExecSpec(
        private val execSpec: ExecSpec,
        globalOptions: GlobalHelmOptions,
        command: String,
        subcommand: String?)
    : HelmExecSpec {


    init {
        execSpec.executable = globalOptions.executable.getOrElse("helm")

        execSpec.args(command)
        subcommand?.let { execSpec.args(it) }

        environment("HELM_HOME", globalOptions.home)
        flag("--debug", globalOptions.debug)
    }


    override fun args(vararg args: Any) {
        execSpec.args(*args)
    }


    override fun args(provider: Provider<out Any>) {
        provider.ifPresent { value ->
            if (value is Collection<*>) {
                execSpec.args(*value.toTypedArray())
            } else {
                execSpec.args(value)
            }
        }
    }


    override fun flag(name: String, value: Boolean, defaultValue: Boolean) {
        if (value != defaultValue) {
            execSpec.args(if (value) name else "$name=false")
        }
    }


    override fun flag(name: String, provider: Provider<Boolean>, defaultValue: Boolean) {
        provider.orNull
                ?.takeIf { it != defaultValue }
                ?.let { value ->
                    execSpec.args(if (value) name else "$name=false")
                }
    }


    override fun option(name: String, value: Any) {
        execSpec.args(name, value)
    }


    override fun option(name: String, provider: Provider<out Any>) {
        provider.ifPresent { value ->
            option(name, value)
        }
    }


    override fun environment(name: String, provider: Provider<out Any>) {
        provider.ifPresent { value ->
            execSpec.environment(name, value)
        }
    }


    override fun assertSuccess(assertSuccess: Boolean) {
        execSpec.isIgnoreExitValue = !assertSuccess
    }


    override fun withExecSpec(action: Action<ExecSpec>) {
        action.execute(execSpec)
    }
}
