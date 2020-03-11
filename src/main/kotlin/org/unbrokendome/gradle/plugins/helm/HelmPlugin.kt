package org.unbrokendome.gradle.plugins.helm

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskDependency
import org.unbrokendome.gradle.plugins.helm.command.HelmCommandsPlugin
import org.unbrokendome.gradle.plugins.helm.command.tasks.AbstractHelmInstallationCommandTask
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmUpdateRepositories
import org.unbrokendome.gradle.plugins.helm.dsl.Filtering
import org.unbrokendome.gradle.plugins.helm.dsl.HelmChart
import org.unbrokendome.gradle.plugins.helm.dsl.HelmExtension
import org.unbrokendome.gradle.plugins.helm.dsl.HelmRepository
import org.unbrokendome.gradle.plugins.helm.dsl.Linting
import org.unbrokendome.gradle.plugins.helm.dsl.createFiltering
import org.unbrokendome.gradle.plugins.helm.dsl.createLinting
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.CertificateCredentials
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.credentials
import org.unbrokendome.gradle.plugins.helm.dsl.dependencies.ChartDependencyHandler
import org.unbrokendome.gradle.plugins.helm.dsl.dependencies.createChartDependencyHandler
import org.unbrokendome.gradle.plugins.helm.dsl.filtering
import org.unbrokendome.gradle.plugins.helm.dsl.helm
import org.unbrokendome.gradle.plugins.helm.dsl.helmChartContainer
import org.unbrokendome.gradle.plugins.helm.dsl.helmRepositoryHandler
import org.unbrokendome.gradle.plugins.helm.dsl.lint
import org.unbrokendome.gradle.plugins.helm.dsl.repositories
import org.unbrokendome.gradle.plugins.helm.rules.AddRepositoryTaskRule
import org.unbrokendome.gradle.plugins.helm.rules.BuildDependenciesTaskRule
import org.unbrokendome.gradle.plugins.helm.rules.ChartDirArtifactRule
import org.unbrokendome.gradle.plugins.helm.rules.ChartPackagedArtifactRule
import org.unbrokendome.gradle.plugins.helm.rules.FilterChartSourcesTaskRule
import org.unbrokendome.gradle.plugins.helm.rules.LintTaskRule
import org.unbrokendome.gradle.plugins.helm.rules.MainChartRule
import org.unbrokendome.gradle.plugins.helm.rules.PackageTaskRule
import org.unbrokendome.gradle.plugins.helm.rules.dirArtifactConfigurationName
import org.unbrokendome.gradle.plugins.helm.rules.packageTaskName
import org.unbrokendome.gradle.plugins.helm.rules.packagedArtifactConfigurationName
import org.unbrokendome.gradle.plugins.helm.rules.registerTaskName
import org.unbrokendome.gradle.plugins.helm.util.booleanProviderFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.fileProviderFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.providerFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.toUri


class HelmPlugin
    : Plugin<Project> {

    internal companion object {
        const val addRepositoriesTaskName = "helmAddRepositories"
        const val updateRepositoriesTaskName = "helmUpdateRepositories"
    }


    override fun apply(project: Project) {

        project.plugins.apply(HelmCommandsPlugin::class.java)

        configureRepositories(project)
        createFilteringExtension(project)
        configureCharts(project)
    }


    /**
     * Performs modifications on the project related to Helm repositories.
     *
     * @param project the current Gradle [Project]
     */
    private fun configureRepositories(project: Project) {
        val repositories = createRepositoriesExtension(project)

        project.tasks.addRule(AddRepositoryTaskRule(project.tasks, repositories))

        val addRepositoriesTask = project.tasks.register(addRepositoriesTaskName) { task ->
            task.group = HELM_GROUP
            task.description = "Registers all configured Helm repositories."
            task.dependsOn(TaskDependency {
                repositories.map { repository ->
                    project.tasks.getByName(repository.registerTaskName)
                }.toSet()
            })
        }

        val updateRepositoriesTask =
            project.tasks.register(updateRepositoriesTaskName, HelmUpdateRepositories::class.java) { task ->
                task.dependsOn(addRepositoriesTask)
            }

        // helm install/upgrade tasks that reference a symbolic repository name should depend on
        // helmUpdateRepositories
        project.tasks.withType(AbstractHelmInstallationCommandTask::class.java) { task ->
            task.dependsOn(TaskDependency {
                if (task.chart.getOrElse("").contains('/')) {
                    setOf(updateRepositoriesTask.get())
                } else emptySet()
            })
        }
    }


    /**
     * Performs modifications on the project related to Helm charts.
     *
     * @param project the current Gradle [Project]
     */
    private fun configureCharts(project: Project) {

        val charts = createChartsExtension(project)

        charts.all { chart ->
            chart.createExtensions(project)
        }

        charts.addRule(MainChartRule(project, charts))

        project.tasks.run {
            addRule(FilterChartSourcesTaskRule(this, charts))
            addRule(BuildDependenciesTaskRule(this, charts))
            addRule(LintTaskRule(this, charts))
            addRule(PackageTaskRule(this, charts))

            create("helmPackage") { task ->
                task.group = HELM_GROUP
                task.description = "Packages all Helm charts."
                task.dependsOn(TaskDependency {
                    charts.map { chart ->
                        project.tasks.getByName(chart.packageTaskName)
                    }.toSet()
                })
            }
        }

        project.configurations.run {
            addRule(ChartDirArtifactRule(project, charts))
            addRule(ChartPackagedArtifactRule(project, charts))
        }

        project.afterEvaluate {

            createRepositoriesFromProjectProperties(project)

            // Realize the main chart.
            charts.findByName("main")

            // Realize the artifact configurations for each chart, so other project can depend on them
            // (rules are not evaluated for cross-project dependencies)
            charts.forEach { chart ->
                project.configurations.findByName(chart.dirArtifactConfigurationName)
                project.configurations.findByName(chart.packagedArtifactConfigurationName)
            }
        }
    }


    /**
     * Creates and installs the `helm.repositories` sub-extension.
     */
    private fun createRepositoriesExtension(project: Project) =
        project.helmRepositoryHandler()
            .apply {
                (project.helm as ExtensionAware)
                    .extensions.add(HELM_REPOSITORIES_EXTENSION_NAME, this)
            }


    /**
     * Creates and installs the `helm.charts` sub-extension.
     */
    private fun createChartsExtension(project: Project): NamedDomainObjectContainer<HelmChart> {
        val helm = project.helm
        return project.helmChartContainer(baseOutputDir = helm.outputDir)
            .apply {
                (helm as ExtensionAware)
                    .extensions.add(HELM_CHARTS_EXTENSION_NAME, this)
            }
    }


    /**
     * Creates and installs the `helm.filtering` sub-extension.
     */
    private fun createFilteringExtension(project: Project) =
        project.objects.createFiltering()
            .apply {
                enabled.set(
                    project.booleanProviderFromProjectProperty("helm.filtering.enabled", defaultValue = true)
                )
                placeholderPrefix.set(
                    project.providerFromProjectProperty("helm.filtering.placeholderPrefix", defaultValue = "\${")
                )
                placeholderSuffix.set(
                    project.providerFromProjectProperty("helm.filtering.placeholderSuffix", defaultValue = "}")
                )

                (project.helm as ExtensionAware)
                    .extensions.add(Filtering::class.java, HELM_FILTERING_EXTENSION_NAME, this)
            }


    /**
     * Creates and installs all the extensions on a [HelmChart].
     *
     * @receiver the [HelmChart] on which to install the extensions
     * @param project the current Gradle [Project]
     */
    private fun HelmChart.createExtensions(project: Project) {
        createFilteringExtension(project.objects, project.helm)
        createLintingExtension(project.objects, project.helm)
        createDependenciesExtension(project)
    }


    /**
     * Creates and installs the `filtering` extension on a [HelmChart].
     *
     * @receiver the [HelmChart] on which to install the extension
     * @param objectFactory the current project's [ObjectFactory]
     * @param helmExtension the global [HelmExtension] (used to inherit values)
     */
    private fun HelmChart.createFilteringExtension(objectFactory: ObjectFactory, helmExtension: HelmExtension) {
        (this as ExtensionAware).extensions
            .add(
                Filtering::class.java,
                "filtering",
                objectFactory.createFiltering(parent = helmExtension.filtering)
            )
    }


    /**
     * Creates and installs the `lint` extension on a [HelmChart].
     *
     * @receiver the [HelmChart] on which to install the extension
     * @param objectFactory the current project's [ObjectFactory]
     * @param helmExtension the global [HelmExtension] (used to inherit values)
     */
    private fun HelmChart.createLintingExtension(objectFactory: ObjectFactory, helmExtension: HelmExtension) {
        (this as ExtensionAware).extensions
            .add(
                Linting::class.java,
                "lint",
                objectFactory.createLinting(parent = helmExtension.lint)
            )
    }


    /**
     * Creates and installs the `lint` extension on a [HelmChart].
     *
     * @receiver the [HelmChart] on which to install the extension
     * @param project the current Gradle [Project]
     */
    private fun HelmChart.createDependenciesExtension(project: Project) {
        (this as ExtensionAware).extensions
            .add(
                ChartDependencyHandler::class.java,
                "dependencies",
                createChartDependencyHandler(this, project)
            )
    }


    /**
     * Creates and registers repositories based on project properties
     * (usually injected through the _gradle.properties_ file).
     *
     * @param project the current Gradle [Project]
     */
    private fun createRepositoriesFromProjectProperties(project: Project) {
        project.properties.keys
            .filter { it.startsWith("helm.repositories.") }
            .mapNotNull { it.split('.').drop(2).firstOrNull() }
            .distinct()
            .forEach { repositoryName ->
                createRepositoryFromProjectProperties(project, repositoryName)
            }
    }


    /**
     * Creates and registers a single [HelmRepository] based on project properties
     * (usually injected through the _gradle.properties_ file).
     *
     * @param project the current Gradle [Project]
     * @param name the name of the repository
     */
    private fun createRepositoryFromProjectProperties(project: Project, name: String) {
        val prefix = "helm.repositories.$name"
        project.helm.repositories
            .create(name) { repository ->
                repository.url.set(
                    project.providerFromProjectProperty("$prefix.url").toUri()
                )

                if (project.hasProperty("$prefix.credentials.username")) {
                    repository.credentials {
                        username.set(project.providerFromProjectProperty("$prefix.credentials.username"))
                        password.set(project.providerFromProjectProperty("$prefix.credentials.password"))
                    }
                } else if (project.hasProperty("$prefix.credentials.certificateFile")) {
                    repository.credentials(CertificateCredentials::class) {
                        certificateFile.set(project.fileProviderFromProjectProperty("$prefix.credentials.certificateFile"))
                        keyFile.set(project.fileProviderFromProjectProperty("$prefix.credentials.keyFile"))
                    }
                }
            }
    }
}
