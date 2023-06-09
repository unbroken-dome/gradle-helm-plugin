package com.citi.gradle.plugins.helm.testutil.exec

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.prop


fun GradleExecMock.eachInvocation(
    matcher: (Invocation) -> Boolean = { true },
    block: StatefulInvocation.() -> Unit
) {
    invocations
        .filter(matcher)
        .filterIsInstance<StatefulInvocation>()
        .forEach(block)
}


fun GradleExecMock.verifyNoInvocations() =
    assertThat(this).prop("invocations") { it.invocations }.isEmpty()


fun GradleExecMock.singleInvocation(block: StatefulInvocation.() -> Unit) {
    assertThat(this).prop(GradleExecMock::invocations).hasSize(1)
    (invocations.singleOrNull() as? StatefulInvocation)?.let(block)
}
