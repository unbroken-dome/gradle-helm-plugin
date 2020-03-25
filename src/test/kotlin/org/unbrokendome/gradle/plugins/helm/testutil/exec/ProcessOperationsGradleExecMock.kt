package org.unbrokendome.gradle.plugins.helm.testutil.exec

import io.mockk.Answer
import io.mockk.Call
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.spyk
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.internal.ProcessOperations
import org.gradle.api.internal.project.DefaultProject
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.gradle.process.internal.ExecActionFactory


interface ProcessOperationsGradleExecMock : GradleExecMock {

    fun answer(slot: CapturingSlot<Action<ExecSpec>>): Answer<ExecResult>


    companion object {

        fun create(execActionFactory: ExecActionFactory): ProcessOperationsGradleExecMock =
            DefaultProcessOperationsGradleExecMock(execActionFactory)


        fun create(project: Project): ProcessOperationsGradleExecMock {
            project as DefaultProject
            val execActionFactory = project.services.get(ExecActionFactory::class.java)
            return create(execActionFactory)
        }
    }
}


private class DefaultProcessOperationsGradleExecMock(
    private val execActionFactory: ExecActionFactory
) : ProcessOperationsGradleExecMock {

    private class Behavior(
        val argsPrefix: List<String>,
        val block: GradleExecResultBuilder.() -> Unit
    )

    private val behaviors = mutableListOf<Behavior>()
    private val allInvocations = mutableListOf<Invocation>()


    private inner class Prefixed(
        val prefix: List<String>
    ) : GradleExecMock {

        override fun forCommand(argsPrefix: List<String>): GradleExecMock =
            Prefixed(this.prefix + argsPrefix)


        override fun everyExec(block: GradleExecResultBuilder.() -> Unit) {
            behaviors.add(Behavior(prefix, block))
        }

        override val invocations: List<Invocation>
            get() = allInvocations.filter { it.args.startsWith(prefix) }


        override fun toString(): String = buildString {
            val invocations = invocations
            append("GradleExecMock for prefix: $prefix")
            if (invocations.isEmpty()) {
                append(" (no recorded invocations)")
            } else {
                appendln()
                appendln("    Recorded invocations (${invocations.size}):")
                appendln(formatInvocations(invocations))
            }
            val otherInvocations = allInvocations - invocations
            if (otherInvocations.isNotEmpty()) {
                appendln("    Other invocations (${otherInvocations.size}):")
                appendln(formatInvocations(otherInvocations))
            }
        }
    }


    override val invocations: List<Invocation>
        get() = allInvocations


    override fun forCommand(argsPrefix: List<String>): GradleExecMock =
        Prefixed(argsPrefix)


    override fun everyExec(block: GradleExecResultBuilder.() -> Unit) {
        behaviors.add(Behavior(emptyList(), block))
    }


    override fun answer(slot: CapturingSlot<Action<ExecSpec>>): Answer<ExecResult> = object : Answer<ExecResult> {

        override fun answer(call: Call): ExecResult {

            val action = slot.captured

            val execSpec = execActionFactory.newExecAction()
            action.execute(execSpec)

            val execAnswerBuilder = GradleExecResultBuilder.fromExecSpec(execSpec)

            behaviors
                .filter { it.argsPrefix.isEmpty() || execSpec.args.startsWith(it.argsPrefix) }
                .forEach { execAnswerBuilder.run(it.block) }

            val result = execAnswerBuilder.buildExecResult()

            if (!execSpec.isIgnoreExitValue) {
                result.assertNormalExitValue()
            }

            allInvocations.add(ExecSpecInvocation(execSpec))

            return result
        }
    }


    private fun formatInvocations(invocations: List<Invocation>) =
        invocations.joinToString(separator = "\n") { "    - ${it.args}"}


    override fun toString(): String = buildString {
        append("GradleExecMock")
        if (allInvocations.isEmpty()) {
            append(" (no recorded invocations)")
        } else {
            appendln()
            appendln("    Recorded invocations (${allInvocations.size}):")
            appendln(formatInvocations(allInvocations))
        }
    }
}


private fun <T> List<T>.startsWith(elements: Iterable<T>) =
    elements.withIndex().all { (index, element) ->
        size > index && get(index) == element
    }


private class ExecSpecInvocation(private val execSpec: ExecSpec) : AbstractInvocation() {

    override val executable: String
        get() = execSpec.executable

    override val args: List<String>
        get() = execSpec.args

    override val environment: Map<String, String>
        get() = execSpec.environment.mapValues { (_, v) -> v.toString() }
}



internal fun ProcessOperations.spyWith(execMock: ProcessOperationsGradleExecMock): ProcessOperations =
    spyk(this) {
        val slot = CapturingSlot<Action<ExecSpec>>()
        every { exec(capture(slot)) } answers execMock.answer(slot)
    }


internal fun ProcessOperationsGradleExecMock.install(project: Project) {
    // We cannot just wrap the Project with a Spy, because the Gradle tasks etc. will still call exec() on the
    // inner Project object. Luckily Project.exec() delegates to a ProcessOperations which we can replace with
    // a spy by reflection. Needless to say that this reaches down into the implementation details of Gradle
    // and might break with a future Gradle version.
    val processOperationsSpy = (project as DefaultProject).processOperations.spyWith(this)

    project.javaClass.getDeclaredField("__processOperations__").run {
        isAccessible = true
        set(project, processOperationsSpy)
    }
}
