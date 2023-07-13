package com.citi.gradle.plugins.helm.publishing.tests.functional

import java.io.File

/**
 * Helping class creating helm executable, which copies tgz file to a destination position.
 *
 * Is needed to simulate real helm (we don't need to verify this) and ignore actions done by other parts of a plugin.
 */
internal object HelmExecutable {
    private val helmExecutablesDirectory = File("./src/functionalTest/resources/executable")
    private val fileExtension = let {
        val isWindows = System.getProperty("os.name").contains("Windows", ignoreCase = true)

        if (isWindows) {
            "bat"
        } else {
            "sh"
        }
    }

    private val executableFileToCreateTgz = File(helmExecutablesDirectory, "helm-create-tgz.$fileExtension")

    private val sourceTgzFile = File(helmExecutablesDirectory, "tgz-file-template.txt")

    /**
     * Function does multiple things:
     * 1. Prepares shell script which will copy tgz file in the temp folder
     * 2. Creates gradle parameter with path to that executable
     */
    fun getExecutableParameterForChartCreation(
        temporaryFolder: File,
        tgzFileDestination: File
    ): HelmExecutableParameter {
        val destinationExecutableFile = File(temporaryFolder, executableFileToCreateTgz.name)

        destinationExecutableFile.writer().use { writer ->
            executableFileToCreateTgz.forEachLine { line ->
                val newLine = line
                    .replace("%TGZ_SOURCE%", sourceTgzFile.normalize().absolutePath)
                    .replace("%TGZ_DESTINATION%", tgzFileDestination.normalize().absolutePath)

                writer.appendLine(newLine)
            }
        }

        destinationExecutableFile.setExecutable(true)

        return HelmExecutableParameter(destinationExecutableFile)
    }

    private fun getAbsoluteNormalizedPath(file: File): String {
        return file
            .absoluteFile
            .normalize()
            .path
            .replace('\\', '/')
    }

    internal class HelmExecutableParameter(
        path: File
    ) {
        val parameterValue = let {
            val normalizedFilePath = getAbsoluteNormalizedPath(path)

            "-Phelm.executable=${normalizedFilePath}"
        }
    }
}