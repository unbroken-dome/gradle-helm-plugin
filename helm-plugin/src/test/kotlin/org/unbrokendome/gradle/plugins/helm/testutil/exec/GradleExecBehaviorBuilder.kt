package com.citi.gradle.plugins.helm.testutil.exec

import java.io.PrintWriter


interface GradleExecBehaviorBuilder {


    fun printsOnStdout(block: (PrintWriter) -> Unit)


    fun printsOnStdout(s: String, newLine: Boolean = false) {
        printsOnStdout { pw ->
            pw.print(s)
            if (newLine) pw.println()
        }
    }
}
