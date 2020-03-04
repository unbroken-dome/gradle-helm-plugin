package org.unbrokendome.gradle.plugins.helm.command.tasks

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory


abstract class AbstractHelmDependenciesTask : AbstractHelmCommandTask() {

    /**
     * The chart directory.
     */
    @get:Internal("Represented as part of other properties")
    val chartDir: DirectoryProperty =
        project.objects.directoryProperty()


    /**
     * Path to the _Chart.yaml_ file of the chart (read-only).
     */
    @get:InputFile
    @Suppress("unused")
    val chartYamlFile: Provider<RegularFile> =
        chartDir.file("Chart.yaml")


    /**
     * The _charts_ sub-directory; this is where sub-charts will be placed by the command (read-only).
     */
    @get:OutputDirectory
    @Suppress("unused")
    val subchartsDir: Provider<Directory> =
        chartDir.dir("charts")
}
