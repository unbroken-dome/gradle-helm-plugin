package org.unbrokendome.gradle.plugins.helm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction


abstract class HelmCollectChartDependencies : DefaultTask() {

    @get:InputFiles
    abstract val dependencies: ConfigurableFileCollection


    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty


    @TaskAction
    fun collectDependencies() {

        val result = project.sync { spec ->
            spec.includeEmptyDirs = false
            spec.into(outputDir)

            dependencies.forEach { dependencyPackageFile ->
                spec.from(project.tarTree(dependencyPackageFile))
            }
        }

        didWork = result.didWork
    }
}
