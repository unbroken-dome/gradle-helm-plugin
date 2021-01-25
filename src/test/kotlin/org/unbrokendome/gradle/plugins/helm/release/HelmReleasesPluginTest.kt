package org.unbrokendome.gradle.plugins.helm.release

import assertk.all
import assertk.assertThat
import assertk.assertions.isSuccess
import assertk.assertions.prop
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmInstallOrUpgrade
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmTest
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmUninstall
import org.unbrokendome.gradle.plugins.helm.dsl.helm
import org.unbrokendome.gradle.plugins.helm.release.dsl.HelmRelease
import org.unbrokendome.gradle.plugins.helm.release.dsl.HelmReleaseTarget
import org.unbrokendome.gradle.plugins.helm.release.dsl.activeReleaseTarget
import org.unbrokendome.gradle.plugins.helm.release.dsl.releaseTargets
import org.unbrokendome.gradle.plugins.helm.release.dsl.releases
import org.unbrokendome.gradle.plugins.helm.spek.propertyMappingInfo
import org.unbrokendome.gradle.plugins.helm.spek.propertyMappingTests
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.cast
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.containsTask
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.doesNotContainItem
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.doesNotHaveTaskDependency
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.hasExtension
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.hasExtensionNamed
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.hasOnlyTaskDependency
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.hasTaskDependency
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.hasValueEqualTo
import org.unbrokendome.gradle.pluginutils.test.evaluate
import org.unbrokendome.gradle.pluginutils.test.spek.applyPlugin
import org.unbrokendome.gradle.pluginutils.test.spek.setupGradleProject
import java.net.URI
import java.time.Duration


object HelmReleasesPluginTest : Spek({

    val project: Project by setupGradleProject { applyPlugin<HelmReleasesPlugin>() }
    val helm by memoized { project.helm }


    describe("when applying the helm-releases plugin") {

        it("project can be evaluated successfully") {
            assertThat {
                project.evaluate()
            }.isSuccess()
        }
    }


    describe("DSL extensions") {

        it("should create a helm.releases extension") {
            assertThat(helm, name = "helm")
                .hasExtension<NamedDomainObjectContainer<HelmRelease>>("releases")
        }


        it("should create a helm.releaseTargets extension") {
            assertThat(helm, name = "helm")
                .hasExtension<NamedDomainObjectContainer<HelmReleaseTarget>>("releaseTargets")
        }


        it("should create a helm.activeReleaseTarget extension property") {
            assertThat(helm, name = "helm")
                .hasExtension<Property<String>>("activeReleaseTarget")
        }


        it("should create a helmInstall task") {
            assertThat(project)
                .containsTask<Task>("helmInstall")
        }


        it("should create a helmUninstall task") {
            assertThat(project)
                .containsTask<Task>("helmUninstall")
        }
    }


    fun createRelease() =
        helm.releases.create("awesome") { release ->
            release.releaseName.set("awesome-release")
            release.from("my-repo/awesome-chart")
            release.version.set("3.42.19")
        }


    fun createReleaseTarget() =
        helm.releaseTargets.create("local")


    describe("when adding a release") {

        beforeEachTest {
            createRelease()
        }

        val release by memoized { helm.releases.getByName("awesome") }


        it("should create a task to install the release to each target") {

            assertThat(project, name = "project")
                .containsTask<HelmInstallOrUpgrade>("helmInstallAwesomeToDefault")
                .all {
                    prop(HelmInstallOrUpgrade::releaseName).hasValueEqualTo("awesome-release")
                    prop(HelmInstallOrUpgrade::chart).hasValueEqualTo("my-repo/awesome-chart")
                    prop(HelmInstallOrUpgrade::version).hasValueEqualTo("3.42.19")
                }
        }


        it("should create a task to install the release to the active target") {

            assertThat(project, name = "project")
                .containsTask<Task>("helmInstallAwesome")
                .hasOnlyTaskDependency("helmInstallAwesomeToDefault")
        }


        it("install-all-to-target task should depend on the install-release-to-target task") {

            assertThat(project, name = "project")
                .containsTask<Task>("helmInstallToDefault")
                .hasOnlyTaskDependency("helmInstallAwesomeToDefault")
        }


        it("should create a task to uninstall the release from each target") {

            assertThat(project, name = "project")
                .containsTask<HelmUninstall>("helmUninstallAwesomeFromDefault")
                .prop(HelmUninstall::releaseName).hasValueEqualTo("awesome-release")
        }


        it("should create a task to uninstall the release from the active target") {

            assertThat(project, name = "project")
                .containsTask<Task>("helmUninstallAwesome")
                .hasOnlyTaskDependency("helmUninstallAwesomeFromDefault")
        }


        it("uninstall-all-from-target task should depend on the uninstall-release-from-target task") {

            assertThat(project, name = "project")
                .containsTask<Task>("helmUninstallFromDefault")
                .hasOnlyTaskDependency("helmUninstallAwesomeFromDefault")
        }


        it("should create a task to test the release on each target") {

            assertThat(project, name = "project")
                .containsTask<HelmTest>("helmTestAwesomeOnDefault")
                .all {
                    prop(HelmTest::releaseName).hasValueEqualTo("awesome-release")
                }
        }


        it("should create a task to test the release on the active target") {

            assertThat(project, name = "project")
                .containsTask<Task>("helmTestAwesome")
                .hasOnlyTaskDependency("helmTestAwesomeOnDefault")
        }


        it("test-all-on-target task should depend on the test-release-on-target task") {

            assertThat(project, name = "project")
                .containsTask<Task>("helmTestOnDefault")
                .hasOnlyTaskDependency("helmTestAwesomeOnDefault")
        }


        describe("install task should use properties from the release") {

            propertyMappingTests<HelmRelease, HelmInstallOrUpgrade>(
                { release },
                "helmInstallAwesomeToDefault",
                propertyMappingInfo(
                    HelmRelease::releaseName, HelmInstallOrUpgrade::releaseName, "awesome-release"
                ),
                propertyMappingInfo(
                    HelmRelease::version, HelmInstallOrUpgrade::version, "3.42.19"
                ),
                propertyMappingInfo(
                    HelmRelease::kubeConfig, HelmInstallOrUpgrade::kubeConfig, "local.kubeconfig"
                ),
                propertyMappingInfo(
                    HelmRelease::kubeContext, HelmInstallOrUpgrade::kubeContext, "local-kubecontext"
                ),
                propertyMappingInfo(
                    HelmRelease::namespace, HelmInstallOrUpgrade::namespace, "custom-namespace"
                ),
                propertyMappingInfo(HelmRelease::dryRun, HelmInstallOrUpgrade::dryRun, true),
                propertyMappingInfo(HelmRelease::noHooks, HelmInstallOrUpgrade::noHooks, true),
                propertyMappingInfo(
                    HelmRelease::remoteTimeout, HelmInstallOrUpgrade::remoteTimeout, Duration.ofSeconds(42)
                ),
                propertyMappingInfo(HelmRelease::atomic, HelmInstallOrUpgrade::atomic, true),
                propertyMappingInfo(HelmRelease::devel, HelmInstallOrUpgrade::devel, true),
                propertyMappingInfo(HelmRelease::verify, HelmInstallOrUpgrade::verify, true),
                propertyMappingInfo(HelmRelease::wait, HelmInstallOrUpgrade::wait, true),
                propertyMappingInfo(HelmRelease::version, HelmInstallOrUpgrade::version, "1.2.3"),
                propertyMappingInfo(HelmRelease::repository, HelmInstallOrUpgrade::repository, URI.create("http://charts.example.com")),
                propertyMappingInfo(HelmRelease::username, HelmInstallOrUpgrade::username, "john.doe"),
                propertyMappingInfo(HelmRelease::password, HelmInstallOrUpgrade::password, "topsecret"),
                propertyMappingInfo(HelmRelease::caFile, HelmInstallOrUpgrade::caFile, "ca.pem"),
                propertyMappingInfo(HelmRelease::certFile, HelmInstallOrUpgrade::certFile, "cert.pem"),
                propertyMappingInfo(HelmRelease::keyFile, HelmInstallOrUpgrade::keyFile, "key.pem")
            )
        }


        describe("uninstall task should use properties from the release") {

            propertyMappingTests<HelmRelease, HelmUninstall>(
                { release },
                "helmUninstallAwesomeFromDefault",
                propertyMappingInfo(
                    HelmRelease::releaseName, HelmUninstall::releaseName, "awesome-release"
                ),
                propertyMappingInfo(
                    HelmRelease::kubeConfig, HelmUninstall::kubeConfig, "local.kubeconfig"
                ),
                propertyMappingInfo(
                    HelmRelease::kubeContext, HelmUninstall::kubeContext, "local-kubecontext"
                ),
                propertyMappingInfo(
                    HelmRelease::namespace, HelmUninstall::namespace, "custom-namespace"
                ),
                propertyMappingInfo(HelmRelease::dryRun, HelmUninstall::dryRun, true),
                propertyMappingInfo(HelmRelease::noHooks, HelmUninstall::noHooks, true),
                propertyMappingInfo(
                    HelmRelease::remoteTimeout, HelmUninstall::remoteTimeout, Duration.ofSeconds(42)
                ),
                propertyMappingInfo(
                    HelmRelease::keepHistoryOnUninstall, HelmUninstall::keepHistory, true
                )
            )
        }


        describe("test task should use properties from the release") {

            propertyMappingTests<HelmRelease, HelmTest>(
                { release },
                "helmTestAwesomeOnDefault",
                propertyMappingInfo(
                    HelmRelease::releaseName, HelmTest::releaseName, "awesome-release"
                ),
                propertyMappingInfo(
                    HelmRelease::kubeConfig, HelmTest::kubeConfig, "local.kubeconfig"
                ),
                propertyMappingInfo(
                    HelmRelease::kubeContext, HelmTest::kubeContext, "local-kubecontext"
                ),
                propertyMappingInfo(
                    HelmRelease::namespace, HelmTest::namespace, "custom-namespace"
                ),
                propertyMappingInfo(
                    { test.showLogs.set(it) }, HelmTest::showLogs, true
                ),
                propertyMappingInfo(
                    { test.timeout.set(it) }, HelmTest::remoteTimeout, Duration.ofSeconds(42)
                )
            )
        }
    }


    describe("active release target") {

        it("can be configured via project property") {

            project.extensions.extraProperties.set("helm.release.target", "test-target")

            assertThat(project.helm, name = "helm")
                .hasExtensionNamed("activeReleaseTarget")
                .cast<Property<String>>()
                .hasValueEqualTo("test-target")
        }
    }


    describe("when adding a release target") {

        beforeEachTest {
            createReleaseTarget()
        }


        it("should create a task to install all releases to the target") {

            assertThat(project, name = "project")
                .containsTask<Task>("helmInstallToLocal")
        }


        it("should create a task to uninstall all releases from the target") {

            assertThat(project, name = "project")
                .containsTask<Task>("helmUninstallFromLocal")
        }


        it("should create a task to test all releases on the target") {

            assertThat(project, name = "project")
                .containsTask<Task>("helmTestOnLocal")
        }


        it("should not create a default target automatically") {

            assertThat(helm.releaseTargets, name = "releaseTargets")
                .doesNotContainItem("default")
        }


        describe("when the target is active") {

            beforeEachTest {
                helm.activeReleaseTarget.set("local")
            }


            it("install-all task should depend on the install-all-to-target task") {

                assertThat(project, name = "project")
                    .containsTask<Task>("helmInstall")
                    .hasOnlyTaskDependency("helmInstallToLocal")
            }


            it("uninstall-all task should depend on the uninstall-all-from-target task") {

                assertThat(project, name = "project")
                    .containsTask<Task>("helmUninstall")
                    .hasOnlyTaskDependency("helmUninstallFromLocal")
            }


            it("test-all task should depend on the test-all-on-target task") {

                assertThat(project, name = "project")
                    .containsTask<Task>("helmTest")
                    .hasOnlyTaskDependency("helmTestOnLocal")
            }
        }
    }


    describe("when adding a release and release target") {

        beforeEachTest {
            createRelease()
            createReleaseTarget()
        }

        val releaseTarget by memoized { helm.releaseTargets.getByName("local") }


        it("install-all-to-target task should depend on the install-release-to-target task") {

            assertThat(project, name = "project")
                .containsTask<Task>("helmInstallToLocal")
                .hasOnlyTaskDependency("helmInstallAwesomeToLocal")
        }


        describe("install task should use properties from the release target") {

            propertyMappingTests<HelmReleaseTarget, HelmInstallOrUpgrade>(
                { releaseTarget },
                "helmInstallAwesomeToLocal",
                propertyMappingInfo(
                    HelmReleaseTarget::kubeConfig, HelmInstallOrUpgrade::kubeConfig, "local.kubeconfig"
                ),
                propertyMappingInfo(
                    HelmReleaseTarget::kubeContext, HelmInstallOrUpgrade::kubeContext, "local-kubecontext"
                ),
                propertyMappingInfo(
                    HelmReleaseTarget::namespace, HelmInstallOrUpgrade::namespace, "custom-namespace"
                ),
                propertyMappingInfo(HelmReleaseTarget::dryRun, HelmInstallOrUpgrade::dryRun, true),
                propertyMappingInfo(HelmReleaseTarget::noHooks, HelmInstallOrUpgrade::noHooks, true),
                propertyMappingInfo(
                    HelmReleaseTarget::remoteTimeout, HelmInstallOrUpgrade::remoteTimeout, Duration.ofSeconds(42)
                ),
                propertyMappingInfo(HelmReleaseTarget::atomic, HelmInstallOrUpgrade::atomic, true),
                propertyMappingInfo(HelmReleaseTarget::devel, HelmInstallOrUpgrade::devel, true),
                propertyMappingInfo(HelmReleaseTarget::verify, HelmInstallOrUpgrade::verify, true),
                propertyMappingInfo(HelmReleaseTarget::wait, HelmInstallOrUpgrade::wait, true)
            )
        }


        describe("uninstall task should use properties from the release target") {

            propertyMappingTests<HelmReleaseTarget, HelmUninstall>(
                { releaseTarget },
                "helmUninstallAwesomeFromLocal",
                propertyMappingInfo(
                    HelmReleaseTarget::kubeConfig, HelmUninstall::kubeConfig, "local.kubeconfig"
                ),
                propertyMappingInfo(
                    HelmReleaseTarget::kubeContext, HelmUninstall::kubeContext, "local-kubecontext"
                ),
                propertyMappingInfo(
                    HelmReleaseTarget::namespace, HelmUninstall::namespace, "custom-namespace"
                ),
                propertyMappingInfo(HelmReleaseTarget::dryRun, HelmUninstall::dryRun, true),
                propertyMappingInfo(HelmReleaseTarget::noHooks, HelmUninstall::noHooks, true),
                propertyMappingInfo(
                    HelmReleaseTarget::remoteTimeout, HelmUninstall::remoteTimeout, Duration.ofSeconds(42)
                )
            )
        }


        describe("test task should use properties from the release target") {

            propertyMappingTests<HelmReleaseTarget, HelmTest>(
                { releaseTarget },
                "helmTestAwesomeOnLocal",
                propertyMappingInfo(
                    HelmReleaseTarget::kubeConfig, HelmTest::kubeConfig, "local.kubeconfig"
                ),
                propertyMappingInfo(
                    HelmReleaseTarget::kubeContext, HelmTest::kubeContext, "local-kubecontext"
                ),
                propertyMappingInfo(
                    HelmReleaseTarget::namespace, HelmTest::namespace, "custom-namespace"
                ),
                propertyMappingInfo(
                    { test.showLogs.set(it) }, HelmTest::showLogs, true
                ),
                propertyMappingInfo(
                    { test.timeout.set(it) }, HelmTest::remoteTimeout, Duration.ofSeconds(42)
                )
            )
        }
    }


    describe("release tags") {

        describe("given an untagged release") {

            beforeEachTest {
                with(project.helm.releases) {
                    create("untagged") { release ->
                        release.from("my-repo/awesome-chart")
                    }
                }
            }


            it("untagged release is included with an always-match selector") {

                project.extensions.extraProperties.set("helm.release.tags", "*")

                assertThat(project, name = "project")
                    .containsTask<Task>("helmInstallToDefault")
                    .hasTaskDependency("helmInstallUntaggedToDefault")
            }


            it("untagged release is not included when a tag is given") {

                project.extensions.extraProperties.set("helm.release.tags", "tag")

                assertThat(project, name = "project")
                    .containsTask<Task>("helmInstallToDefault")
                    .doesNotHaveTaskDependency("helmInstallUntaggedToDefault")
            }


            it("untagged release is not included when the release target selects on a tag") {

                with(project.helm.releaseTargets) {
                    named("default") { it.selectTags = "tag" }
                }

                assertThat(project, name = "project")
                    .containsTask<Task>("helmInstallToDefault")
                    .doesNotHaveTaskDependency("helmInstallUntaggedToDefault")
            }
        }


        describe("given a tagged release") {

            beforeEachTest {
                with(project.helm.releases) {
                    create("tagged") { release ->
                        release.from("my-repo/awesome-chart")
                        release.tags("awesome")
                    }
                }
            }


            it("tagged release is included when tag matches") {

                project.extensions.extraProperties.set("helm.release.tags", "awesome")

                assertThat(project, name = "project")
                    .containsTask<Task>("helmInstallToDefault")
                    .hasTaskDependency("helmInstallTaggedToDefault")
            }


            it("tagged release is not included when it does not match global tag expression") {

                project.extensions.extraProperties.set("helm.release.tags", "different")

                assertThat(project, name = "project")
                    .containsTask<Task>("helmInstallToDefault")
                    .doesNotHaveTaskDependency("helmInstallTaggedToDefault")
            }


            it("tagged release is not included when it does not match the release target tag expression") {

                with(project.helm.releaseTargets) {
                    named("default") { it.selectTags = "different" }
                }

                assertThat(project, name = "project")
                    .containsTask<Task>("helmInstallToDefault")
                    .doesNotHaveTaskDependency("helmInstallUntaggedToDefault")
            }


            it("tagged release is included when any tag matches") {

                with(project.helm.releases) {
                    named("tagged") { it.tags("another") }
                }

                project.extensions.extraProperties.set("helm.release.tags", "another")

                assertThat(project, name = "project")
                    .containsTask<Task>("helmInstallToDefault")
                    .hasTaskDependency("helmInstallTaggedToDefault")
            }
        }


        describe("release target and global tag expressions are combined") {

            beforeEachTest {
                with (project.helm.releases) {
                    create("taggedFoo") { release ->
                        release.from("my-repo/awesome-chart")
                        release.tags("foo")
                    }
                    create("taggedBar") { release ->
                        release.from("my-repo/awesome-chart")
                        release.tags("bar")
                    }
                    create("taggedFooAndBar") { release ->
                        release.from("my-repo/awesome-chart")
                        release.tags("foo", "bar")
                    }
                }

                with(project.helm.releaseTargets) {
                    named("default") {
                        it.selectTags = "foo"
                    }
                }
            }


            it("should include only releases matching both expressions") {
                project.extensions.extraProperties.set("helm.release.tags", "bar")

                assertThat(project, name = "project")
                    .containsTask<Task>("helmInstallToDefault")
                    .hasOnlyTaskDependency("helmInstallTaggedFooAndBarToDefault")
            }

        }
    }
})
