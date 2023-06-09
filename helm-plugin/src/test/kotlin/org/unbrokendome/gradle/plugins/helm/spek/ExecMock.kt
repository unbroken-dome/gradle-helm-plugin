package com.citi.gradle.plugins.helm.spek

import org.gradle.api.Project
import org.spekframework.spek2.dsl.LifecycleAware
import org.spekframework.spek2.lifecycle.CachingMode
import org.spekframework.spek2.lifecycle.MemoizedValue
import com.citi.gradle.plugins.helm.command.HelmCommandsPlugin
import com.citi.gradle.plugins.helm.dsl.internal.helm
import com.citi.gradle.plugins.helm.testutil.exec.DefaultExecutableGradleExecMock
import com.citi.gradle.plugins.helm.testutil.exec.ExecutableGradleExecMock
import com.citi.gradle.plugins.helm.testutil.exec.GradleExecMock
import com.citi.gradle.plugins.helm.testutil.exec.withStatefulVerification


/**
 * Uses a [GradleExecMock] for tests that invoke external processes.
 *
 * @param executableFileName the name of the fake executable file. This will be created as an executable shell script
 *        inside the project directory. Exec actions must set their `executable` property to this path.
 * @return a [GradleExecMock] as a Spek [MemoizedValue]
 */
fun LifecycleAware.gradleExecMock(executableFileName: String = "helm"): MemoizedValue<GradleExecMock> {

    val executableExecMock: ExecutableGradleExecMock by memoized(
        mode = CachingMode.SCOPE,
        factory = {
            val execMock: ExecutableGradleExecMock = DefaultExecutableGradleExecMock()
            execMock.start()
            execMock
        },
        destructor = { it.close() }
    )

    beforeEachTest {
        val project: Project by memoized()
        val scriptFilePath = project.projectDir.resolve(executableFileName)

        executableExecMock.createScriptFile(scriptFilePath)

        project.plugins.withType(HelmCommandsPlugin::class.java) {
            project.helm.executable.set(scriptFilePath.absolutePath)
        }
    }

    afterEachTest {
        executableExecMock.reset()
    }

    return memoized(mode = CachingMode.TEST) {
        executableExecMock.withStatefulVerification()
    }
}
