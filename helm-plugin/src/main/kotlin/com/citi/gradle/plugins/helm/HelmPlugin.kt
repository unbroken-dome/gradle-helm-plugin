package com.citi.gradle.plugins.helm

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskDependency
import com.citi.gradle.plugins.helm.command.HelmCommandsPlugin
import com.citi.gradle.plugins.helm.command.tasks.AbstractHelmInstallationCommandTask
import com.citi.gradle.plugins.helm.command.tasks.HelmUpdateRepositories
import com.citi.gradle.plugins.helm.dsl.*
import com.citi.gradle.plugins.helm.dsl.credentials.CertificateCredentials
import com.citi.gradle.plugins.helm.dsl.credentials.credentials
import com.citi.gradle.plugins.helm.dsl.dependencies.ChartDependencyHandler
import com.citi.gradle.plugins.helm.dsl.dependencies.createChartDependencyHandler
import com.citi.gradle.plugins.helm.dsl.internal.filtering
import com.citi.gradle.plugins.helm.dsl.internal.helm
import com.citi.gradle.plugins.helm.dsl.internal.lint
import com.citi.gradle.plugins.helm.dsl.internal.repositories
import com.citi.gradle.plugins.helm.rules.*
import org.unbrokendome.gradle.pluginutils.booleanProviderFromProjectProperty
import org.unbrokendome.gradle.pluginutils.fileProviderFromProjectProperty
import org.unbrokendome.gradle.pluginutils.providerFromProjectProperty
import org.unbrokendome.gradle.pluginutils.toUri


class HelmPlugin
    : Plugin<Project> {

    internal companion object {
        const val addRepositoriesTaskName = "helmAddRepositories"
        const val updateRepositoriesTaskName = "helmUpdateRepositories"
    }


    override fun apply(project: Project) {

        project.plugins.apply(HelmCommandsPlugin::class.java)

        configureRepositories(project)
        project.configureFiltering()
        project.configureCharts()
    }


    /**
     * Performs modifications on the project related to Helm repositories.
     *
     * @param project the current Gradle [Project]
     */
    private fun configureRepositories(project: Project) {
        val repositories = project.createRepositoriesExtension()

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
                task.repositoryNames.set(project.provider { repositories.names })
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
     * @receiver the current Gradle [Project]
     */
    private fun Project.configureCharts() {

        val charts = this.createChartsExtension()

        charts.all { chart ->
            chart.createExtensions(this)
        }

        charts.addRule(MainChartRule(this, charts))

        with (tasks) {
            addRule(FilterChartSourcesTaskRule(this, charts))
            addRule(CollectChartDependenciesTaskRule(this, charts))
            addRule(CollectChartSourcesTaskRule(this, charts))
            addRule(UpdateDependenciesTaskRule(this, charts))
            addRule(LintTaskRule(this, charts))
            addRule(LintWithConfigurationTaskRule(this, charts))
            addRule(PackageTaskRule(this, charts))
            addRule(RenderTaskRule(this, charts))
            addRule(RenderAllTaskRule(this, charts))
        }

        tasks.register("helmPackage") { task ->
            task.group = HELM_GROUP
            task.description = "Packages all Helm charts."
            task.dependsOn(TaskDependency {
                charts.mapTo(mutableSetOf()) { chart ->
                    tasks.getByName(chart.packageTaskName)
                }
            })
        }

        tasks.register("helmRender") { task ->
            task.group = HELM_GROUP
            task.description = "Renders all renderings of all Helm charts."
            task.dependsOn(TaskDependency {
                charts.mapTo(mutableSetOf()) { chart ->
                    tasks.getByName(chart.renderAllTaskName)
                }
            })
        }

        configurations.addRule(ChartDependenciesConfigurationRule(configurations, charts))
        configurations.addRule(ChartDirArtifactRule(configurations, tasks, charts))
        configurations.addRule(ChartPackagedArtifactRule(configurations, tasks, charts))

        afterEvaluate { p ->

            p.createRepositoriesFromProjectProperties()

            // Realize the main chart.
            charts.findByName("main")

            // Realize the artifact configurations for each chart, so other projects can depend on them
            // (rules are not evaluated for cross-project dependencies)
            charts.forEach { chart ->
                configurations.findByName(chart.dirArtifactConfigurationName)
                configurations.findByName(chart.packagedArtifactConfigurationName)
            }
        }
    }


    /**
     * Creates and installs the `helm.repositories` sub-extension.
     */
    private fun Project.createRepositoriesExtension() =
        helmRepositoryHandler()
            .apply {
                (helm as ExtensionAware)
                    .extensions.add(HELM_REPOSITORIES_EXTENSION_NAME, this)
            }


    /**
     * Creates and installs the `helm.charts` sub-extension.
     */
    private fun Project.createChartsExtension(): NamedDomainObjectContainer<HelmChart> {
        val helm = helm as HelmExtensionInternal

        return helmChartContainer(
                baseOutputDir = helm.outputDir,
                globalRenderBaseOutputDir = helm.renderOutputDir,
                filteredSourcesBaseDir = helm.tmpDir.dir("filtered"),
                dependenciesBaseDir = helm.tmpDir.dir("dependencies")
            )
            .apply {
                (helm as ExtensionAware)
                    .extensions.add(HELM_CHARTS_EXTENSION_NAME, this)
            }
    }


    /**
     * Creates and installs the `helm.filtering` sub-extension.
     */
    private fun Project.configureFiltering() {
        val filtering = objects.createFiltering()
            .apply {
                enabled.convention(
                    booleanProviderFromProjectProperty("helm.filtering.enabled", defaultValue = true)
                )
            }
        (helm as ExtensionAware).extensions
            .add(Filtering::class.java, HELM_FILTERING_EXTENSION_NAME, filtering)
    }


    /**
     * Creates and installs all the extensions on a [HelmChart].
     *
     * @receiver the [HelmChart] on which to install the extensions
     * @param project the current Gradle [Project]
     */
    private fun HelmChart.createExtensions(project: Project) {
        createFilteringExtension(project, project.helm)
        createLintingExtension(project.objects, project.helm)
        createDependenciesExtension(project)
    }


    /**
     * Creates and installs the `filtering` extension on a [HelmChart].
     *
     * @receiver the [HelmChart] on which to install the extension
     * @param project the current Gradle [Project]
     * @param helmExtension the global [HelmExtension] (used to inherit values)
     */
    private fun HelmChart.createFilteringExtension(project: Project, helmExtension: HelmExtension) {
        (this as ExtensionAware).extensions
            .add(
                Filtering::class.java,
                HELM_FILTERING_EXTENSION_NAME,
                project.objects.createFiltering(parent = helmExtension.filtering)
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
                HELM_LINT_EXTENSION_NAME,
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
                HELM_DEPENDENCIES_EXTENSION_NAME,
                createChartDependencyHandler(this, project)
            )
    }


    /**
     * Creates and registers repositories based on project properties
     * (usually injected through the _gradle.properties_ file).
     *
     * @receiver the current Gradle [Project]
     */
    private fun Project.createRepositoriesFromProjectProperties() {
        properties.keys
            .filter { it.startsWith("helm.repositories.") }
            .mapNotNull { it.split('.').drop(2).firstOrNull() }
            .distinct()
            .forEach { repositoryName ->
                createRepositoryFromProjectProperties(repositoryName)
            }
    }


    /**
     * Creates and registers a single [HelmRepository] based on project properties
     * (usually injected through the _gradle.properties_ file).
     *
     * @receiver the current Gradle [Project]
     * @param name the name of the repository
     */
    private fun Project.createRepositoryFromProjectProperties(name: String) {
        val prefix = "helm.repositories.$name"
        helm.repositories
            .create(name) { repository ->
                repository.url.set(
                    providerFromProjectProperty("$prefix.url").toUri()
                )

                if (hasProperty("$prefix.credentials.username")) {
                    repository.credentials { cred ->
                        cred.username.set(providerFromProjectProperty("$prefix.credentials.username"))
                        cred.password.set(providerFromProjectProperty("$prefix.credentials.password"))
                    }
                } else if (hasProperty("$prefix.credentials.certificateFile")) {
                    repository.credentials(CertificateCredentials::class) {
                        certificateFile.set(fileProviderFromProjectProperty("$prefix.credentials.certificateFile"))
                        keyFile.set(fileProviderFromProjectProperty("$prefix.credentials.keyFile"))
                    }
                }
            }
    }
}
