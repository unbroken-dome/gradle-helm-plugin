package org.unbrokendome.gradle.plugins.helm.testutil.exec


interface Invocation {

    val commandLine: List<String>
        get() = listOf(executable) + args

    val executable: String

    val args: List<String>

    val environment: Map<String, String>
}


abstract class AbstractInvocation : Invocation {

    override fun toString(): String =
        commandLine.toString()
}


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
    fun everyExec(block: GradleExecResultBuilder.() -> Unit)


    /**
     * The list of recorded invocations of this exec mock.
     */
    val invocations: List<Invocation>
}

