package com.citi.gradle.plugins.helm.testutil.exec

import assertk.Assert
import assertk.all
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.key
import assertk.assertions.prop
import assertk.assertions.support.expected
import assertk.assertions.support.show
import java.util.BitSet


private typealias StringAssertion = Assert<String>.() -> Unit


/**
 * "Stateful" variant of [Invocation] that removes arguments from the command line when they are checked. This
 * allows us to verify that there are no arguments besides the ones that were verified.
 */
class StatefulInvocation(
    private val invocation: Invocation
) : AbstractInvocation(), Invocation by invocation {

    private var commandAssert: StringAssertion = { }
    private var subcommandAssert: StringAssertion? = null
    private val argAssertions = mutableListOf<StringAssertion>()
    private var executableAssert: StringAssertion = { }
    private val expectedFlags = mutableListOf<String>()
    private val expectedOptions = mutableMapOf<String, StringAssertion>()
    private val expectedEnvironment = mutableMapOf<String, StringAssertion>()


    fun expectExecutable(valueAssert: StringAssertion) {
        executableAssert = valueAssert
    }


    fun expectExecutable(value: String) =
        expectExecutable { isEqualTo(value) }


    fun expectCommand(
        commandAssert: StringAssertion,
        subcommandAssert: StringAssertion? = null
    ) {
        this.commandAssert = commandAssert
        this.subcommandAssert = subcommandAssert
    }


    fun expectCommand(command: String, subcommand: String? = null) {
        this.commandAssert = { isEqualTo(command) }
        this.subcommandAssert = subcommand?.let { { isEqualTo(it) } }
    }


    fun expectArg(valueAssert: StringAssertion) {
        argAssertions.add(valueAssert)
    }


    fun expectArg(value: String) =
        expectArg { isEqualTo(value) }


    fun expectFlag(flag: String) {
        expectedFlags.add(flag)
    }


    fun expectOption(name: String, valueAssert: StringAssertion) {
        expectedOptions.merge(name, valueAssert) { assertion1, assertion2 ->
            { all { assertion1(); assertion2() } }
        }
    }


    fun expectOption(name: String, value: String) =
        expectOption(name) { isEqualTo(value) }


    fun expectEnvironment(name: String, valueAssert: StringAssertion) {
        expectedEnvironment.merge(name, valueAssert) { assertion1, assertion2 ->
            { all { assertion1(); assertion2() } }
        }
    }


    fun expectEnvironment(name: String, value: String) =
        expectEnvironment(name) { isEqualTo(value) }


    internal fun verify(assertion: Assert<StatefulInvocation>) {

        val seen = BitSet(args.size)

        assertion.all {
            prop(Invocation::executable).executableAssert()

            hasCommand(commandAssert, seen)

            subcommandAssert?.let { subcommandAssert ->
                hasSubcommand(subcommandAssert, seen)
            }

            for (expectedFlag in expectedFlags) {
                hasFlag(expectedFlag, seen)
            }

            for ((optionName, valueAssert) in expectedOptions) {
                option(optionName, seen).valueAssert()
            }

            for (argAssertion in argAssertions) {
                arg(seen).isNotNull().argAssertion()
            }

            for ((varName, valueAssert) in expectedEnvironment) {
                prop(Invocation::environment).key(varName).valueAssert()
            }

            hasNoMoreArgs(seen)
        }
    }
}


fun Assert<StatefulInvocation>.meetsExpectations() = given { actual ->
    actual.verify(this)
}


private fun Assert<Invocation>.hasCommand(commandAssertion: StringAssertion, seen: BitSet) = given { actual ->
    val actualCommand = actual.args.firstOrNull()
        ?.also { seen.set(0) }
    assertThat(actualCommand, name = "command").isNotNull().commandAssertion()
}


private fun Assert<Invocation>.hasSubcommand(subcommandAssertion: StringAssertion, seen: BitSet) = given { actual ->
    val actualSubcommand = actual.args.getOrNull(1)
        ?.also { seen.set(1) }
    assertThat(actualSubcommand, name = "subcommand").isNotNull().subcommandAssertion()
}


private fun Assert<Invocation>.hasFlag(value: String, seen: BitSet) = given { actual ->
    val index = actual.args.indexOf(value)
    if (index == -1) {
        expected("to contain flag ${show(name)}")
    }
    seen.set(index)
}


private fun Assert<Invocation>.option(name: String, seen: BitSet) = transform { actual ->
    val index = actual.args.indexOf(name)
    if (index == -1) {
        expected("to contain option ${show(name)}")
    }
    seen.set(index)
    val value = actual.args.getOrNull(index + 1)
        ?: expected("to contain option ${show(name)}, but did not have a value")
    seen.set(index + 1)

    value
}


private fun Assert<Invocation>.arg(seen: BitSet) = transform { actual ->
    val firstUnseen = seen.nextClearBit(0)
    if (firstUnseen < actual.args.size) {
        actual.args[firstUnseen].also { seen.set(firstUnseen) }
    } else null
}


private fun Assert<Invocation>.hasNoMoreArgs(seen: BitSet) = given { actual ->
    if (seen.cardinality() < actual.args.size) {
        val remaining = actual.args.filterIndexed { index, _ -> !seen[index] }
        expected("to have no more arguments, but had remaining: ${show(remaining)}")
    }
}


fun GradleExecMock.withStatefulVerification(): GradleExecMock =
    if (this is GradleExecMockWithStatefulVerification) {
        this
    } else {
        GradleExecMockWithStatefulVerification(this)
    }


private class GradleExecMockWithStatefulVerification(
    private val delegate: GradleExecMock
) : GradleExecMock by delegate {

    private val statefulInvocations by lazy {
        delegate.invocations.associateWith { StatefulInvocation(it) }
    }


    override fun forCommand(argsPrefix: List<String>): GradleExecMock =
        Prefixed(delegate.forCommand(argsPrefix))


    override val invocations: List<Invocation>
        get() = statefulInvocations.values.toList()


    private inner class Prefixed(
        private val delegate: GradleExecMock
    ) : GradleExecMock by delegate {


        override fun forCommand(argsPrefix: List<String>): GradleExecMock =
            Prefixed(delegate.forCommand(argsPrefix))


        override val invocations: List<Invocation>
            get() = delegate.invocations.map { statefulInvocations.getValue(it) }
    }


    override fun toString(): String =
        delegate.toString()
}
