package com.citi.gradle.plugins.helm.testutil.exec


/**
 * Represents a single invocation of an external process, which can be verified by the test.
 */
interface Invocation {

    /**
     * The entire command line, including the executable and all arguments.
     */
    val commandLine: List<String>
        get() = listOf(executable) + args

    /**
     * The executable of the invocation.
     */
    val executable: String

    /**
     * The command line arguments (not including the executable).
     */
    val args: List<String>

    /**
     * The environment variables.
     */
    val environment: Map<String, String>
}


abstract class AbstractInvocation : Invocation {

    override fun toString(): String =
        commandLine.toString()
}


class DefaultInvocation(
    override val executable: String,
    override val args: List<String>,
    override val environment: Map<String, String>
) : AbstractInvocation()


interface GradleExecMock {

    /**
     * Returns a new [GradleExecMock] that is scoped for invocations starting with the given arguments
     * (not counting the executable).
     *
     * * calls to [everyExec] will only match on calls starting with the given arguments
     * * the list of [invocations] will only return invocations starting with the given arguments
     *
     * @param argsPrefix the arguments prefix
     */
    fun forCommand(argsPrefix: List<String>): GradleExecMock


    /**
     * Returns a new [GradleExecMock] that is scoped for invocations starting with the given arguments
     * (not counting the executable).
     *
     * * calls to [everyExec] will only match on calls starting with the given arguments
     * * the list of [invocations] will only return invocations starting with the given arguments
     *
     * @param argsPrefix the arguments prefix
     */
    fun forCommand(vararg argsPrefix: String) =
        forCommand(argsPrefix.toList())


    /**
     * Sets up mocking behavior.
     */
    fun everyExec(block: GradleExecBehaviorBuilder.() -> Unit)


    /**
     * The list of recorded invocations of this exec mock.
     */
    val invocations: List<Invocation>
}

