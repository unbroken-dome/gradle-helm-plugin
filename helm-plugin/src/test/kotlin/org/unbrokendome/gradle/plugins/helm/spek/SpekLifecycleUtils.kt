package com.citi.gradle.plugins.helm.spek

import org.spekframework.spek2.Spek
import org.spekframework.spek2.dsl.GroupBody
import org.spekframework.spek2.dsl.Root
import org.spekframework.spek2.dsl.Skip
import org.spekframework.spek2.dsl.TestBody
import org.spekframework.spek2.lifecycle.CachingMode
import org.spekframework.spek2.lifecycle.LifecycleListener


interface ExecutionResultAwareGroupBody : GroupBody {

    fun afterEachSuccessfulTest(handler: TestBody.() -> Unit)

    fun afterEachFailedTest(handler: TestBody.(cause: Throwable) -> Unit)
}


interface ExecutionResultAwareRoot : Root, ExecutionResultAwareGroupBody


fun GroupBody.afterEachSuccessfulTest(handler: TestBody.() -> Unit) =
    (this as ExecutionResultAwareGroupBody).afterEachSuccessfulTest(handler)


fun GroupBody.afterEachFailedTest(handler: TestBody.(cause: Throwable) -> Unit) =
    (this as ExecutionResultAwareGroupBody).afterEachFailedTest(handler)


fun GroupBody.executionResultAware(block: ExecutionResultAwareGroupBody.() -> Unit) {
    val adapter = (this as? ExecutionResultAwareGroupBody) ?: ExecutionResultAwareGroupBodyAdapter(this)
    adapter.block()
}

fun Root.executionResultAware(block: ExecutionResultAwareRoot.() -> Unit) {
    val adapter = (this as? ExecutionResultAwareRoot) ?: ExecutionResultAwareRootAdapter(this)
    adapter.block()
}

fun executionResultAware(root: ExecutionResultAwareRoot.() -> Unit): Root.() -> Unit = {
    this.executionResultAware(root)
}


abstract class ExecutionResultAwareSpek(root: ExecutionResultAwareRoot.() -> Unit) : Spek(executionResultAware(root))


private class ExecutionResultAwareGroupBodyAdapter(
    private val delegate: GroupBody
) : ExecutionResultAwareGroupBody,
    GroupBody by delegate {

    private val afterEachSuccessfulTestHandlers = mutableListOf<TestBody.() -> Unit>()
    private val afterEachFailedTestHandlers = mutableListOf<TestBody.(Throwable) -> Unit>()


    override fun afterEachSuccessfulTest(handler: TestBody.() -> Unit) {
        afterEachSuccessfulTestHandlers.add(handler)
    }


    override fun afterEachFailedTest(handler: TestBody.(cause: Throwable) -> Unit) {
        afterEachFailedTestHandlers.add(handler)
    }


    override fun group(
        description: String,
        skip: Skip,
        defaultCachingMode: CachingMode,
        preserveExecutionOrder: Boolean,
        failFast: Boolean,
        body: GroupBody.() -> Unit
    ) {
        delegate.group(description, skip, defaultCachingMode, preserveExecutionOrder, failFast) {
            ExecutionResultAwareGroupBodyAdapter(this).body()
        }
    }


    override fun test(description: String, skip: Skip, timeout: Long, body: TestBody.() -> Unit) {
        delegate.test(description, skip, timeout) {
            try {
                body()

                this@ExecutionResultAwareGroupBodyAdapter.afterEachSuccessfulTestHandlers.forEach { handler ->
                    this.handler()
                }

            } catch (e: Throwable) {
                this@ExecutionResultAwareGroupBodyAdapter.afterEachFailedTestHandlers.forEach { handler ->
                    this.handler(e)
                }
                throw e
            }
        }
    }
}


private class ExecutionResultAwareRootAdapter(
    private val delegate: Root
) : ExecutionResultAwareGroupBody by ExecutionResultAwareGroupBodyAdapter(delegate),
    ExecutionResultAwareRoot {

    override fun registerListener(listener: LifecycleListener) =
        delegate.registerListener(listener)
}
