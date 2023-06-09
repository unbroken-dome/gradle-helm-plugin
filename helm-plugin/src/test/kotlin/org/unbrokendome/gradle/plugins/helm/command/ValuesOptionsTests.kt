package com.citi.gradle.plugins.helm.command

import org.gradle.api.Project
import org.gradle.api.Task
import com.citi.gradle.plugins.helm.testutil.exec.GradleExecMock
import com.citi.gradle.plugins.helm.testutil.exec.Invocation
import com.citi.gradle.plugins.helm.testutil.exec.eachInvocation


class ValuesOptionsTests(vararg commands: String) : AbstractOptionsTests({

    val project: Project by memoized()
    val task: Task by memoized()
    val valuesOptions: ConfigurableHelmValueOptions by memoized { task as ConfigurableHelmValueOptions }
    val execMock: GradleExecMock by memoized()

    fun Invocation.matchesCommand(): Boolean =
        args.firstOrNull() in commands


    variant("with values property") {

        beforeEachTest {
            with(valuesOptions) {
                values.put("stringProperty", "stringValue")
                values.put("otherProperty", "otherValue")
                values.put("numberProperty", 42)
            }
        }

        afterEachTest {
            execMock.eachInvocation(Invocation::matchesCommand) {
                expectOption("--set-string", "stringProperty=stringValue,otherProperty=otherValue")
                expectOption("--set", "numberProperty=42")
            }
        }
    }


    variant("with fileValues property") {

        beforeEachTest {
            valuesOptions.fileValues.put("property", project.file("value.txt"))
        }

        afterEachTest {
            execMock.eachInvocation(Invocation::matchesCommand) {
                expectOption("--set-file", "property=${project.projectDir}/value.txt")
            }
        }
    }


    variant("with valueFiles property") {

        beforeEachTest {
            valuesOptions.valueFiles.from("values.yaml", "moreValues.yaml")
        }

        afterEachTest {
            execMock.eachInvocation(Invocation::matchesCommand) {
                expectOption("--values", "${project.projectDir}/values.yaml,${project.projectDir}/moreValues.yaml")
            }
        }
    }
})
