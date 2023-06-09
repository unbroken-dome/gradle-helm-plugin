package com.citi.gradle.plugins.helm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction


open class HelmCollectChartDependencies : DefaultTask() {

    @get:InputFiles
    var dependencies: FileCollection =
        project.layout.files()


    @get:OutputDirectory
    val outputDir: DirectoryProperty =
        project.objects.directoryProperty()


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
