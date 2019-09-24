package org.unbrokendome.gradle.plugins.helm

import assertk.assert
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.unbrokendome.gradle.plugins.helm.dsl.charts
import org.unbrokendome.gradle.plugins.helm.dsl.helm
import org.unbrokendome.gradle.plugins.helm.tasks.HelmFilterSources
import org.unbrokendome.gradle.plugins.helm.testutil.*
import org.yaml.snakeyaml.Yaml


@Suppress("NestedLambdaShadowedImplicitParameter")
class HelmChartDependenciesTest : AbstractGradleProjectTest() {

    @Nested
    @DisplayName("with chart dependency in same project")
    inner class WithChartDependencyInSameProject {

        @Test
        fun `should resolve dependency`() {

            setupProjectFiles()
            project.applyHelmPlugin()

            with(project.helm.charts) {
                create("foo") { chart ->
                    chart.chartName.set("foo")
                    chart.chartVersion.set("1.2.3")
                    chart.sourceDir.set(project.file("src/helm/foo"))
                }

                create("bar") { chart ->
                    chart.chartName.set("bar")
                    chart.chartVersion.set("3.2.1")
                    chart.sourceDir.set(project.file("src/helm/bar"))
                    chart.dependencies.add("fooDep", "foo")
                }
            }

            evaluateProject()

            val filterSources = project.tasks.getByName("helmFilterBarChartSources") as HelmFilterSources
            filterSources.filterSources()

            val filteredRequirements = project.file("${project.buildDir}/helm/charts/bar/requirements.yaml")
                .reader().use { Yaml().load(it) }

            assert(filteredRequirements).isMapOf<String, Any?> {
                it.hasEntry("dependencies") {
                    it.isInstanceOf(List::class) {
                        it.hasSize(1)
                        it.at(0) {
                            it.isMapOf<String, Any?> {
                                it.hasEntry("repository") {
                                    it.isEqualTo("file://../foo")
                                }
                            }
                        }
                    }
                }
            }
        }


        @Test
        fun `should resolve dependency using alias`() {

            setupProjectFiles()
            project.applyHelmPlugin()

            with(project.helm.charts) {
                create("foo") { chart ->
                    chart.chartName.set("foo")
                    chart.chartVersion.set("1.2.3")
                    chart.sourceDir.set(project.file("src/helm/foo"))
                }

                create("bar") { chart ->
                    chart.chartName.set("bar")
                    chart.chartVersion.set("3.2.1")
                    chart.sourceDir.set(project.file("src/helm/bar"))
                    chart.dependencies.add("fooAlias", "foo")
                }
            }

            evaluateProject()

            val filterSources = project.tasks.getByName("helmFilterBarChartSources") as HelmFilterSources
            filterSources.filterSources()

            val filteredRequirements = project.file("${project.buildDir}/helm/charts/bar/requirements.yaml")
                .reader().use { Yaml().load(it) }

            assert(filteredRequirements).isMapOf<String, Any?> {
                it.hasEntry("dependencies") {
                    it.isInstanceOf(List::class) {
                        it.hasSize(1)
                        it.at(0) {
                            it.isMapOf<String, Any?> {
                                it.hasEntry("repository") {
                                    it.isEqualTo("file://../foo")
                                }
                            }
                        }
                    }
                }
            }
        }


        private fun setupProjectFiles() {
            directory(project.projectDir) {
                directory("src/helm/foo") {
                    file(
                        "Chart.yaml", """
                        ---
                        name: foo
                        version: 1.2.3
                        """.trimIndent()
                    )
                }
                directory("src/helm/bar") {
                    file(
                        "Chart.yaml", """
                        ---
                        name: bar
                        version: 3.2.1
                        """.trimIndent()
                    )
                    file(
                        "requirements.yaml", """
                        ---
                        dependencies:
                          - name: fooDep
                            version: "*"
                            alias: fooAlias
                        """.trimIndent()
                    )
                }
            }
        }
    }


    @Nested
    @DisplayName("with chart dependency on another project")
    inner class WithChartDependencyOnAnotherProject {

        private lateinit var fooProject: Project
        private lateinit var barProject: Project

        @BeforeEach
        fun createSubprojects() {
            fooProject = ProjectBuilder.builder()
                .withName("foo")
                .withParent(project)
                .withProjectDir(project.projectDir.resolve("foo"))
                .build()
            fooProject.applyHelmPlugin()

            barProject = ProjectBuilder.builder()
                .withName("bar")
                .withParent(project)
                .withProjectDir(project.projectDir.resolve("bar"))
                .build()
            barProject.applyHelmPlugin()
        }


        @Test
        fun `should resolve dependency`() {
            setupProjectFiles()

            with(barProject.helm.charts.getByName("main")) {
                dependencies.add("foo", project = ":foo")
            }

            evaluateProject()

            val filterSources = barProject.tasks.getByName("helmFilterMainChartSources") as HelmFilterSources
            filterSources.filterSources()

            val filteredRequirements = project.file("${barProject.buildDir}/helm/charts/bar/requirements.yaml")
                .reader().use { Yaml().load(it) }

            assert(filteredRequirements).isMapOf<String, Any?> {
                it.hasEntry("dependencies") {
                    it.isInstanceOf(List::class) {
                        it.hasSize(1)
                        it.at(0) {
                            it.isMapOf<String, Any?> {
                                it.hasEntry("repository") {
                                    it.isEqualTo("file://../../../../../foo/build/helm/charts/foo")
                                }
                            }
                        }
                    }
                }
            }
        }


        @Test
        fun `should resolve dependency using alias`() {
            setupProjectFiles()

            with(barProject.helm.charts.getByName("main")) {
                dependencies.add("fooAlias", project = ":foo")
            }

            evaluateProject()

            val filterSources = barProject.tasks.getByName("helmFilterMainChartSources") as HelmFilterSources
            filterSources.filterSources()

            val filteredRequirements = project.file("${barProject.buildDir}/helm/charts/bar/requirements.yaml")
                .reader().use { Yaml().load(it) }

            assert(filteredRequirements).isMapOf<String, Any?> {
                it.hasEntry("dependencies") {
                    it.isInstanceOf(List::class) {
                        it.hasSize(1)
                        it.at(0) {
                            it.isMapOf<String, Any?> {
                                it.hasEntry("repository") {
                                    it.isEqualTo("file://../../../../../foo/build/helm/charts/foo")
                                }
                            }
                        }
                    }
                }
            }
        }


        private fun setupProjectFiles() {
            directory(fooProject.projectDir) {
                directory("src/main/helm") {
                    file(
                        "Chart.yaml", """
                    ---
                    name: foo
                    version: 1.2.3
                    """.trimIndent()
                    )
                }
            }
            directory(barProject.projectDir) {
                directory("src/main/helm") {
                    file(
                        "Chart.yaml", """
                        ---
                        name: bar
                        version: 3.2.1
                        """.trimIndent()
                    )
                    file(
                        "requirements.yaml", """
                        ---
                        dependencies:
                          - name: foo
                            version: "*"
                            alias: fooAlias
                        """.trimIndent()
                    )
                }
            }
        }
    }


    private fun Project.applyHelmPlugin() {
        project.plugins.apply(HelmPlugin::class.java)
    }
}
