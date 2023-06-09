package com.citi.gradle.plugins.helm.command

import assertk.assertThat
import assertk.assertions.each
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.gradle.api.Project
import org.spekframework.spek2.dsl.GroupBody
import org.spekframework.spek2.dsl.LifecycleAware
import org.spekframework.spek2.style.specification.describe
import com.citi.gradle.plugins.helm.dsl.internal.helm
import com.citi.gradle.plugins.helm.spek.afterEachSuccessfulTest
import com.citi.gradle.plugins.helm.testutil.exec.GradleExecMock
import com.citi.gradle.plugins.helm.testutil.exec.StatefulInvocation
import com.citi.gradle.plugins.helm.testutil.exec.eachInvocation
import com.citi.gradle.plugins.helm.testutil.exec.meetsExpectations


interface OptionsTestProvider {

    fun addTests(groupBody: GroupBody, innerTests: GroupBody.() -> Unit)
}


interface OptionsTestBody : LifecycleAware {

    fun variant(description: String, body: LifecycleAware.() -> Unit)
}


private class DefaultOptionsTestBody(
    private val groupBody: GroupBody,
    private val innerTests: GroupBody.() -> Unit
) : OptionsTestBody, LifecycleAware by groupBody {

    override fun variant(description: String, body: LifecycleAware.() -> Unit) {
        groupBody.describe(description) {
            body(this)
            this@DefaultOptionsTestBody.innerTests.invoke(delegate)
        }
    }
}


abstract class AbstractOptionsTests(
    private val spec: OptionsTestBody.() -> Unit
) : OptionsTestProvider {

    override fun addTests(groupBody: GroupBody, innerTests: GroupBody.() -> Unit) {
        DefaultOptionsTestBody(groupBody, innerTests).spec()
    }
}


fun GroupBody.withOptionsTesting(vararg optionsTestProviders: OptionsTestProvider, spec: GroupBody.() -> Unit) {

    afterEachSuccessfulTest {
        val execMock: GradleExecMock by memoized()

        assertThat(execMock).prop(GradleExecMock::invocations)
            .each {
                it.isInstanceOf(StatefulInvocation::class).meetsExpectations()
            }
    }

    describe("with default options") {
        delegate.spec()
    }

    for (provider in optionsTestProviders) {
        provider.addTests(this, spec)
    }
}


object GlobalOptionsTests : AbstractOptionsTests({

    val project: Project by memoized()
    val execMock: GradleExecMock by memoized()


    variant("with debug flag") {

        beforeEachTest {
            project.helm.debug.set(true)
        }

        afterEachTest {
            execMock.eachInvocation {
                expectFlag("--debug")
            }
        }
    }


    variant("with extra args") {

        beforeEachTest {
            project.helm.extraArgs.addAll("--custom-option", "customValue", "--custom-flag")
        }

        afterEachTest {
            execMock.eachInvocation {
                expectOption("--custom-option", "customValue")
                expectFlag("--custom-flag")
            }
        }
    }


    variant("with custom XDG_* directories") {

        beforeEachTest {
            project.helm.xdgDataHome.set(project.file("custom-data-home"))
            project.helm.xdgConfigHome.set(project.file("custom-config-home"))
            project.helm.xdgCacheHome.set(project.file("custom-cache-home"))
        }

        afterEachTest {
            execMock.eachInvocation {
                expectEnvironment("XDG_DATA_HOME", "${project.projectDir}/custom-data-home")
                expectEnvironment("XDG_CONFIG_HOME", "${project.projectDir}/custom-config-home")
                expectEnvironment("XDG_CACHE_HOME", "${project.projectDir}/custom-cache-home")
            }
        }
    }
})
