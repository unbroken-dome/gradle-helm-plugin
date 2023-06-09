package com.citi.gradle.plugins.helm.release

import org.gradle.api.*
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.reflect.TypeOf
import com.citi.gradle.plugins.helm.*
import com.citi.gradle.plugins.helm.command.internal.conventionsFrom
import com.citi.gradle.plugins.helm.dsl.internal.helm
import com.citi.gradle.plugins.helm.release.dsl.HelmReleaseTarget
import com.citi.gradle.plugins.helm.release.dsl.helmReleaseContainer
import com.citi.gradle.plugins.helm.release.dsl.helmReleaseTargetContainer
import com.citi.gradle.plugins.helm.release.rules.*
import com.citi.gradle.plugins.helm.release.tags.TagExpression
import org.unbrokendome.gradle.pluginutils.booleanProviderFromProjectProperty
import org.unbrokendome.gradle.pluginutils.durationProviderFromProjectProperty
import org.unbrokendome.gradle.pluginutils.property
import org.unbrokendome.gradle.pluginutils.providerFromProjectProperty


class HelmReleasesPlugin : Plugin<Project> {

    internal companion object {
        const val installAllTaskName = "helmInstall"
        const val uninstallAllTaskName = "helmUninstall"
        const val testAllTaskName = "helmTest"
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

                addRule(HelmTestReleaseOnTargetTaskRule(this, releases, releaseTargets))
                addRule(HelmTestOnTargetTaskRule(this, releases, releaseTargets))
                addRule(HelmTestReleaseTaskRule(this, releases, validatedActiveReleaseTarget))

                addRule(HelmStatusReleaseOnTargetTaskRule(this, releases, releaseTargets))
                addRule(HelmStatusReleaseTaskRule(this, releases, validatedActiveReleaseTarget))
            }

            createInstallAllReleasesTask(validatedActiveReleaseTarget)
            createUninstallAllReleasesTask(validatedActiveReleaseTarget)
            createTestAllReleasesTask(validatedActiveReleaseTarget)
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
                val waitForJobs = booleanProviderFromProjectProperty("helm.waitForJobs")

                releaseTargets.all { releaseTarget ->
                    releaseTarget.conventionsFrom(project.helm)
                    releaseTarget.atomic.convention(atomic)
                    releaseTarget.dryRun.convention(dryRun)
                    releaseTarget.noHooks.convention(noHooks)
                    releaseTarget.remoteTimeout.convention(remoteTimeout)
                    releaseTarget.wait.convention(wait)
                    releaseTarget.waitForJobs.convention(waitForJobs)
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
                activeReleaseTarget.map { installAllToTargetTaskName(it) }
            )
        }
    }


    private fun Project.createUninstallAllReleasesTask(
        activeReleaseTarget: Provider<String>
    ) {
        tasks.register(uninstallAllTaskName) { task ->
            task.group = HELM_GROUP
            task.description = "Uninstalls all Helm releases from the active target."

            task.dependsOn(
                activeReleaseTarget.map { uninstallAllFromTargetTaskName(it) }
            )
        }
    }


    private fun Project.createTestAllReleasesTask(
        activeReleaseTarget: Provider<String>
    ) {
        tasks.register(testAllTaskName) { task ->
            task.group = HELM_GROUP
            task.description = "Tests all Helm releases on the active target."

            task.dependsOn(
                activeReleaseTarget.map { testAllOnTargetTaskName(it) }
            )
        }
    }
}
