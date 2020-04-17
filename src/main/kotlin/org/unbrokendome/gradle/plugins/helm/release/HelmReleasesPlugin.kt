package org.unbrokendome.gradle.plugins.helm.release

import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.reflect.TypeOf
import org.unbrokendome.gradle.plugins.helm.HELM_DEFAULT_RELEASE_TARGET
import org.unbrokendome.gradle.plugins.helm.HELM_GROUP
import org.unbrokendome.gradle.plugins.helm.HELM_RELEASES_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.HELM_RELEASE_TARGETS_EXTENSION_NAME
import org.unbrokendome.gradle.plugins.helm.HelmPlugin
import org.unbrokendome.gradle.plugins.helm.command.conventionsFrom
import org.unbrokendome.gradle.plugins.helm.dsl.helm
import org.unbrokendome.gradle.plugins.helm.release.dsl.HelmReleaseTarget
import org.unbrokendome.gradle.plugins.helm.release.dsl.helmReleaseContainer
import org.unbrokendome.gradle.plugins.helm.release.dsl.helmReleaseTargetContainer
import org.unbrokendome.gradle.plugins.helm.release.rules.DefaultReleaseTargetRule
import org.unbrokendome.gradle.plugins.helm.release.rules.HelmInstallReleaseTaskRule
import org.unbrokendome.gradle.plugins.helm.release.rules.HelmInstallReleaseToTargetTaskRule
import org.unbrokendome.gradle.plugins.helm.release.rules.HelmInstallToTargetTaskRule
import org.unbrokendome.gradle.plugins.helm.release.rules.HelmUninstallFromTargetTaskRule
import org.unbrokendome.gradle.plugins.helm.release.rules.HelmUninstallReleaseFromTargetTaskRule
import org.unbrokendome.gradle.plugins.helm.release.rules.HelmUninstallReleaseTaskRule
import org.unbrokendome.gradle.plugins.helm.release.rules.installAllToTargetTaskName
import org.unbrokendome.gradle.plugins.helm.release.rules.uninstallAllFromTargetTaskName
import org.unbrokendome.gradle.plugins.helm.release.tags.TagExpression
import org.unbrokendome.gradle.plugins.helm.util.booleanProviderFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.durationProviderFromProjectProperty
import org.unbrokendome.gradle.plugins.helm.util.property
import org.unbrokendome.gradle.plugins.helm.util.providerFromProjectProperty


class HelmReleasesPlugin : Plugin<Project> {

    internal companion object {
        const val installAllTaskName = "helmInstall"
        const val uninstallAllTaskName = "helmUninstall"
    }


    override fun apply(project: Project) {

        project.run {

            plugins.apply(HelmPlugin::class.java)

            // Add an extension property "activeReleaseTarget" to the helm extension
            val activeReleaseTarget = objects.property<String>()
                .convention(
                    providerFromProjectProperty(
                        "helm.release.target",
                        defaultValue = HELM_DEFAULT_RELEASE_TARGET
                    )
                )
            (helm as ExtensionAware).extensions.add(
                object : TypeOf<Property<String>>() {}, "activeReleaseTarget", activeReleaseTarget
            )

            val releases = createReleasesExtension()
            val releaseTargets = createReleaseTargetsExtension()

            val validatedActiveReleaseTarget = validateReleaseTargetExists(activeReleaseTarget, releaseTargets)

            tasks.run {
                addRule(HelmInstallReleaseToTargetTaskRule(this, releases, releaseTargets))
                addRule(HelmInstallToTargetTaskRule(this, releases, releaseTargets))
                addRule(HelmInstallReleaseTaskRule(this, releases, validatedActiveReleaseTarget))

                addRule(HelmUninstallReleaseFromTargetTaskRule(this, releases, releaseTargets))
                addRule(HelmUninstallFromTargetTaskRule(this, releases, releaseTargets))
                addRule(HelmUninstallReleaseTaskRule(this, releases, validatedActiveReleaseTarget))
            }

            createInstallAllReleasesTask(validatedActiveReleaseTarget)
            createUninstallAllReleasesTask(validatedActiveReleaseTarget)
        }
    }


    /**
     * Return a new [Provider] that returns the same value, but validates that the release target actually exists
     * when it is resolved.
     *
     * This may not be the case if the build script declares custom release targets but the activeTarget is
     * still set to "default" by convention.
     * In this case we want to have a more informative error message than "task X not found" when resolving
     * task dependencies.
     */
    private fun Project.validateReleaseTargetExists(
        provider: Provider<String>,
        releaseTargets: NamedDomainObjectCollection<HelmReleaseTarget>
    ) = provider.map { name ->
            if (name !in releaseTargets.names && (name != HELM_DEFAULT_RELEASE_TARGET || releaseTargets.isNotEmpty())) {
                throw GradleException(
                    "The Helm release target \"${name}\" does not exist in project \"${project.path}\". " +
                            "If you defined release targets in the build script, make sure that " +
                            "the property helm.release.target is set to one of them. " +
                            "Available release targets are: ${releaseTargets.names.toList()}"
                )
            }
            name
        }


    private fun Project.createReleasesExtension() =
        helmReleaseContainer()
            .also { releases ->
                (helm as ExtensionAware)
                    .extensions.add(HELM_RELEASES_EXTENSION_NAME, releases)
                // Note: releases should not be injected with any conventions, this makes it easier to merge
                // with the release target when we build a target-specific release
            }


    private fun Project.createReleaseTargetsExtension(): NamedDomainObjectContainer<HelmReleaseTarget> {

        val globalSelectTagsExpression: TagExpression = TagExpression.fromProvider(
            providerFromProjectProperty("helm.release.tags", defaultValue = "*")
                .map { TagExpression.parse(it) }
        )

        return helmReleaseTargetContainer(globalSelectTagsExpression)
            .also { releaseTargets ->
                (helm as ExtensionAware)
                    .extensions.add(HELM_RELEASE_TARGETS_EXTENSION_NAME, releaseTargets)

                releaseTargets.addRule(DefaultReleaseTargetRule(releaseTargets))

                // Conventions from project properties
                val atomic = booleanProviderFromProjectProperty("helm.atomic")
                val dryRun = booleanProviderFromProjectProperty("helm.dryRun")
                val noHooks = booleanProviderFromProjectProperty("helm.noHooks")
                val remoteTimeout = durationProviderFromProjectProperty("helm.remoteTimeout")
                val wait = booleanProviderFromProjectProperty("helm.wait")

                releaseTargets.all { releaseTarget ->
                    releaseTarget.conventionsFrom(project.helm)
                    releaseTarget.atomic.convention(atomic)
                    releaseTarget.dryRun.convention(dryRun)
                    releaseTarget.noHooks.convention(noHooks)
                    releaseTarget.remoteTimeout.convention(remoteTimeout)
                    releaseTarget.wait.convention(wait)
                }

                // Realize the "default" target so it will be found when enumerating release targets
                afterEvaluate {
                    releaseTargets.findByName(HELM_DEFAULT_RELEASE_TARGET)
                }
            }
    }


    private fun Project.createInstallAllReleasesTask(
        activeReleaseTarget: Provider<String>
    ) {
        tasks.register(installAllTaskName) { task ->
            task.group = HELM_GROUP
            task.description = "Installs all Helm releases to the active target."

            task.dependsOn(
                activeReleaseTarget.map(::installAllToTargetTaskName)
            )
        }
    }


    private fun Project.createUninstallAllReleasesTask(
        activeReleaseTarget: Provider<String>
    ) {
        tasks.register(uninstallAllTaskName) { task ->
            task.group = HELM_GROUP
            task.description = "Uninstalls all Helm releases."

            task.dependsOn(
                activeReleaseTarget.map(::uninstallAllFromTargetTaskName)
            )
        }
    }
}
