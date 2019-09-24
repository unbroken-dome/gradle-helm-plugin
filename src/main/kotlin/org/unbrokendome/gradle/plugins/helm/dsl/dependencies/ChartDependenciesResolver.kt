package org.unbrokendome.gradle.plugins.helm.dsl.dependencies

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import java.io.File


/**
 * Resolves chart dependencies to actual filesystem directories.
 */
internal object ChartDependenciesResolver {

    fun chartDependenciesMap(project: Project, configuredChartName: String?): Map<String, File> {

        if (configuredChartName != null) {

            val configuration = project.configurations.findByName("helm${configuredChartName.capitalize()}Dependencies")
            if (configuration != null) {
                return configuration.helmDependencies
                    .mapValues { (_, dependency) ->
                        when (dependency) {
                            is Configuration ->
                                // dependency on chart in the same project -> no resolution required, just get the artifact
                                dependency.artifacts.files
                                    .first()
                            is Dependency ->
                                // dependency on chart in another project
                                configuration.resolvedConfiguration.getFiles { it == dependency }
                                    .first()
                            else ->
                                throw IllegalStateException(
                                    "Invalid chart dependency: $dependency, " +
                                            "must be of type Configuration or Dependency"
                                )
                        }
                    }
            }
        }

        return emptyMap()
    }
}
