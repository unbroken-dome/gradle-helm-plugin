package org.unbrokendome.gradle.plugins.helm.dsl.dependencies

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.unbrokendome.gradle.plugins.helm.command.execHelm
import org.unbrokendome.gradle.plugins.helm.dsl.helm
import org.yaml.snakeyaml.Yaml
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File


internal sealed class ChartDependency {

    /**
     * Returns a dependency "notation" that can be passed to [DependencyHandler.add].
     */
    abstract val dependencyNotation: Any


    /**
     * Resolves this chart dependency.
     */
    fun resolve(project: Project, configuration: Configuration): ResolvedChartDependency {
        val file = resolveFile(configuration)

        // run "helm inspect chart" to render the Chart.yaml to stdout.
        // this works with directories as well as packaged charts
        val stdout = ByteArrayOutputStream()
        project.helm.execHelm("inspect", "chart") {
            args(file)
            withExecSpec {
                standardOutput = stdout
            }
        }

        val chartProperties = ByteArrayInputStream(stdout.toByteArray()).use { input ->
            @Suppress("UNCHECKED_CAST")
            Yaml().loadAs(input, Map::class.java) as Map<String, Any?>
        }

        return ResolvedChartDependency(
            version = chartProperties["version"].toString(),
            file = file
        )
    }


    /**
     * Resolves this chart dependency to a file.
     *
     * @param configuration the source configuration containing the chart dependency
     * @return a [File] representing the packaged chart artifact
     */
    protected abstract fun resolveFile(configuration: Configuration): File


    class Internal(
        /** The artifact configuration of the target chart. */
        private val artifactConfiguration: Configuration
    ) : ChartDependency() {

        override val dependencyNotation: Any
            get() = artifactConfiguration

        override fun resolveFile(configuration: Configuration): File =
            artifactConfiguration.artifacts.files.first()
    }


    class External(
        /** The dependency on the chart artifact. */
        private val dependency: Dependency
    ) : ChartDependency() {

        override val dependencyNotation: Any
            get() = dependency

        override fun resolveFile(configuration: Configuration): File =
            configuration.resolvedConfiguration.getFiles { it == dependency }.first()
    }
}
