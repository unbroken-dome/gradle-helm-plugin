package org.unbrokendome.gradle.plugins.helm

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.unbrokendome.gradle.plugins.helm.testutil.assertions.jsonPath
import org.unbrokendome.gradle.plugins.helm.testutil.assertions.yamlContents
import org.unbrokendome.gradle.plugins.helm.testutil.directory


class HelmChartDependenciesIntegrationTest : AbstractGradleIntegrationTest() {

    @Nested
    @DisplayName("with chart dependency in same project")
    inner class WithChartDependencyInSameProject {

        @BeforeEach
        fun setupProjectFiles() {
            directory(projectDir) {
                file(
                    "build.gradle", """ 
                    plugins {
                        id('org.unbroken-dome.helm')
                    }
                    
                    helm.charts {
                        foo {
                            chartName = 'foo'
                            chartVersion = '1.2.3'
                            sourceDir = file('src/helm/foo')
                        }
                        bar {
                            chartName = 'bar'
                            chartVersion = '3.2.1'
                            sourceDir = file('src/helm/bar')
                        }
                    }
                    
                """
                )

                directory("src/helm/foo") {
                    file(
                        "Chart.yaml", """
                        ---
                        apiVersion: v2
                        name: foo
                        version: 1.2.3
                        """
                    )
                }
                directory("src/helm/bar") {
                    file(
                        "Chart.yaml", """
                        ---
                        apiVersion: v2
                        name: bar
                        version: 3.2.1
                        dependencies:
                          - name: fooDep
                            version: "*"
                            alias: fooAlias
                        """
                    )
                    file(
                        "requirements.yaml", """
                        ---
                        dependencies:
                          - name: fooDep
                            version: "*"
                            alias: fooAlias
                        """
                    )
                }
            }
        }


        @Test
        fun `should resolve dependency`() {

            directory(projectDir) {
                file(
                    "build.gradle", append = true, contents = """
                    helm.charts {
                        bar { 
                            dependencies {
                                fooDep(chart: 'foo')
                            }
                        }
                    }
                """
                )
            }

            val result = runGradle("helmFilterBarChartSources")

            assertThat(result.task(":helmFilterBarChartSources")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

            val filteredRequirementsFile = buildDir.resolve("helm/charts/bar/requirements.yaml")
            assertThat(filteredRequirementsFile, "requirements.yaml")
                .yamlContents().all {
                    jsonPath<String>("$.dependencies[0].name")
                        .isEqualTo("fooDep")
                    jsonPath<String>("$.dependencies[0].version")
                        .isEqualTo("1.2.3")
                    jsonPath<String>("$.dependencies[0].repository")
                        .isEqualTo("file://../foo")
                }
        }


        @Test
        fun `should resolve dependency using alias`() {

            directory(projectDir) {
                file(
                    "build.gradle", append = true, contents = """
                    helm.charts {
                        bar { 
                            dependencies {
                                fooAlias(chart: 'foo')
                            }
                        }
                    }
                """
                )
            }

            val result = runGradle("helmFilterBarChartSources")

            assertThat(result.task(":helmFilterBarChartSources")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

            val filteredRequirementsFile = buildDir.resolve("helm/charts/bar/requirements.yaml")
            assertThat(filteredRequirementsFile, "requirements.yaml")
                .yamlContents().all {
                    jsonPath<String>("$.dependencies[0].name")
                        .isEqualTo("fooDep")
                    jsonPath<String>("$.dependencies[0].version")
                        .isEqualTo("1.2.3")
                    jsonPath<String>("$.dependencies[0].repository")
                        .isEqualTo("file://../foo")
                }
        }
    }


    @Nested
    @DisplayName("with chart dependency on another project")
    inner class WithChartDependencyOnAnotherProject {

        @BeforeEach
        fun setupProjectFiles() {
            directory(projectDir) {
                file(
                    "settings.gradle", append = true, contents = """
                    include("foo", "bar")
                    """
                )

                directory("foo") {
                    file(
                        "build.gradle", """ 
                        plugins {
                            id('org.unbroken-dome.helm')
                        }
                        
                        helm.charts {
                            main {
                                chartName = 'foo'
                                chartVersion = '1.2.3'
                            }
                        }
                        """
                    )

                    directory("src/main/helm") {
                        file(
                            "Chart.yaml", """
                            ---
                            apiVersion: v1
                            name: foo
                            version: 1.2.3
                            """
                        )
                    }
                }

                directory("bar") {
                    file(
                        "build.gradle", """ 
                        plugins {
                            id('org.unbroken-dome.helm')
                        }
                        
                        helm.charts {
                            main { 
                                chartName = 'bar'
                                chartVersion = '3.2.1'
                            }
                        }
                        
                        """
                    )

                    directory("src/main/helm") {
                        file(
                            "Chart.yaml", """
                            ---
                            apiVersion: v1
                            name: bar
                            version: 3.2.1
                            """
                        )

                        file(
                            "requirements.yaml", """
                            ---
                            dependencies:
                              - name: foo
                                version: "*"
                                alias: fooAlias
                            """
                        )
                    }
                }
            }
        }


        @Test
        fun `should resolve dependency`() {

            directory("$projectDir/bar") {
                file(
                    "build.gradle", append = true, contents = """
                    helm.charts {
                        main { 
                            dependencies {
                                foo(project: ':foo')
                            }
                        }
                    }
                    """
                )
            }

            val result = runGradle(":bar:helmFilterMainChartSources")

            assertThat(result).all {
                taskOutcome(":bar:helmFilterMainChartSources").isSuccess()
                taskOutcome(":foo:helmFilterMainChartSources").isSuccess()
            }

            val filteredRequirementsFile = projectDir.resolve("bar/build/helm/charts/bar/requirements.yaml")
            assertThat(filteredRequirementsFile, "requirements.yaml")
                .yamlContents().all {
                    jsonPath<String>("$.dependencies[0].name")
                        .isEqualTo("foo")
                    jsonPath<String>("$.dependencies[0].version")
                        .isEqualTo("1.2.3")
                    jsonPath<String>("$.dependencies[0].repository")
                        .isEqualTo("file://../../../../../foo/build/helm/charts/foo")
                }
        }


        @Test
        fun `should resolve dependency using alias`() {

            directory("$projectDir/bar") {
                file(
                    "build.gradle", append = true, contents = """
                    helm.charts {
                        main { 
                            dependencies {
                                fooAlias(project: ':foo')
                            }
                        }
                    }
                """
                )
            }

            val result = runGradle(":bar:helmFilterMainChartSources")

            assertThat(result).all {
                taskOutcome(":bar:helmFilterMainChartSources").isSuccess()
                taskOutcome(":foo:helmFilterMainChartSources").isSuccess()
            }

            val filteredRequirementsFile = projectDir.resolve("bar/build/helm/charts/bar/requirements.yaml")
            assertThat(filteredRequirementsFile, "requirements.yaml")
                .yamlContents().all {
                    jsonPath<String>("$.dependencies[0].name")
                        .isEqualTo("foo")
                    jsonPath<String>("$.dependencies[0].version")
                        .isEqualTo("1.2.3")
                    jsonPath<String>("$.dependencies[0].repository")
                        .isEqualTo("file://../../../../../foo/build/helm/charts/foo")
                }
        }
    }
}
