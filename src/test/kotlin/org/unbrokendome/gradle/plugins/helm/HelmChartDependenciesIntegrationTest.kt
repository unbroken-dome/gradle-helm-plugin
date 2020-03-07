package org.unbrokendome.gradle.plugins.helm

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.unbrokendome.gradle.plugins.helm.testutil.assertions.at
import org.unbrokendome.gradle.plugins.helm.testutil.assertions.hasValueEqualTo
import org.unbrokendome.gradle.plugins.helm.testutil.assertions.isNotPresent
import org.unbrokendome.gradle.plugins.helm.testutil.assertions.yamlContents
import org.unbrokendome.gradle.plugins.helm.testutil.directory
import java.io.File
import java.util.stream.Stream


class HelmChartDependenciesIntegrationTest : AbstractGradleIntegrationTest() {

    enum class ChartApiVersion(val value: String, private val description: String, val requirementsFileName: String) {
        V1("v1", "API version v1", "requirements.yaml"),
        V2("v2", "API version v2", "Chart.yaml");

        override fun toString(): String = description
    }


    enum class YamlStyle(private val description: String) {
        BLOCK("YAML block style") {
            override fun renderMapping(pairs: List<Pair<String, String>>, indent: Int) =
                pairs.joinToString(separator = "\n" + " ".repeat(indent)) { (key, value) -> "$key: \"$value\"" }
        },
        FLOW("YAML flow style") {
            override fun renderMapping(pairs: List<Pair<String, String>>, indent: Int): String =
                pairs.joinToString(separator = ", ", prefix = "{", postfix = "}") { (key, value) -> "$key: \"$value\"" }
        };

        abstract fun renderMapping(pairs: List<Pair<String, String>>, indent: Int): String

        fun renderMapping(map: Map<String, String>, indent: Int): String =
            renderMapping(map.toList(), indent)

        override fun toString(): String = description
    }


    class CombinationArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> =
            Stream.of(
                arguments(ChartApiVersion.V1, YamlStyle.BLOCK),
                arguments(ChartApiVersion.V1, YamlStyle.FLOW),
                arguments(ChartApiVersion.V2, YamlStyle.BLOCK),
                arguments(ChartApiVersion.V1, YamlStyle.FLOW)
            )
    }


    @Nested
    @DisplayName("with chart dependency in same project")
    inner class WithChartDependencyInSameProject {

        private fun setupProjectFiles(
            apiVersion: ChartApiVersion, yamlStyle: YamlStyle,
            dependencyProperties: Map<String, String>
        ) {
            directory(projectDir) {
                file(
                    "build.gradle",
                    """ 
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
                        "Chart.yaml",
                        """
                        ---
                        apiVersion: ${apiVersion.value}
                        name: foo
                        version: 1.2.3
                        
                        """
                    )
                }
                directory("src/helm/bar") {
                    file(
                        "Chart.yaml",
                        """
                        ---
                        apiVersion: ${apiVersion.value}
                        name: bar
                        version: 3.2.1
                        
                        """
                    )
                    file(
                        apiVersion.requirementsFileName, append = true,
                        contents = """
                        
                        dependencies:
                          - ${yamlStyle.renderMapping(dependencyProperties, indent = 28)}
                        """
                    )
                }
            }
        }


        @ParameterizedTest
        @ArgumentsSource(CombinationArgumentsProvider::class)
        fun `should resolve dependency`(apiVersion: ChartApiVersion, yamlStyle: YamlStyle) {

            setupProjectFiles(
                apiVersion, yamlStyle,
                mapOf(
                    "name" to "fooDep",
                    "version" to "*"
                )
            )

            directory(projectDir) {
                file(
                    "build.gradle", append = true,
                    contents = """
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

            val filteredRequirementsFile = buildDir.resolve("helm/charts/bar/${apiVersion.requirementsFileName}")
            assertThat(filteredRequirementsFile, apiVersion.requirementsFileName)
                .containsDependency(name = "fooDep", version = "1.2.3", repository = "file://../foo", index = 0)
        }


        @ParameterizedTest
        @ArgumentsSource(CombinationArgumentsProvider::class)
        fun `should resolve dependency and add version if not present`(apiVersion: ChartApiVersion, yamlStyle: YamlStyle) {

            setupProjectFiles(
                apiVersion, yamlStyle,
                mapOf("name" to "fooDep")
            )

            directory(projectDir) {
                file(
                    "build.gradle", append = true,
                    contents = """
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

            val filteredRequirementsFile = buildDir.resolve("helm/charts/bar/${apiVersion.requirementsFileName}")
            assertThat(filteredRequirementsFile, apiVersion.requirementsFileName)
                .containsDependency(name = "fooDep", version = "1.2.3", repository = "file://../foo", index = 0)
        }


        @ParameterizedTest
        @ArgumentsSource(CombinationArgumentsProvider::class)
        fun `should resolve dependency using alias`(apiVersion: ChartApiVersion, yamlStyle: YamlStyle) {

            setupProjectFiles(
                apiVersion, yamlStyle,
                mapOf(
                    "name" to "fooDep",
                    "version" to "*",
                    "alias" to "fooAlias"
                )
            )

            directory(projectDir) {
                file(
                    "build.gradle", append = true,
                    contents = """
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

            val filteredRequirementsFile = buildDir.resolve("helm/charts/bar/${apiVersion.requirementsFileName}")
            assertThat(filteredRequirementsFile, apiVersion.requirementsFileName)
                .containsDependency(
                    name = "fooDep", version = "1.2.3",
                    repository = "file://../foo", alias = "fooAlias", index = 0
                )
        }

    }


    @Nested
    @DisplayName("with chart dependency on another project")
    inner class WithChartDependencyOnAnotherProject {

        private fun setupProjectFiles(
            apiVersion: ChartApiVersion, yamlStyle: YamlStyle,
            dependencyProperties: Map<String, String>
        ) {
            directory(projectDir) {
                file(
                    "settings.gradle", append = true,
                    contents = """
                    include("foo", "bar")
                    """
                )

                directory("foo") {
                    file(
                        "build.gradle",
                        """ 
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
                            "Chart.yaml",
                            """
                            ---
                            apiVersion: ${apiVersion.value}
                            name: foo
                            version: 1.2.3
                            """
                        )
                    }
                }

                directory("bar") {
                    file(
                        "build.gradle",
                        """ 
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
                            apiVersion: ${apiVersion.value}
                            name: bar
                            version: 3.2.1
                            
                            """
                        )

                        file(
                            apiVersion.requirementsFileName, append = true,
                            contents = """
                            
                            dependencies:
                              - ${yamlStyle.renderMapping(dependencyProperties, indent = 32)}
                            """
                        )
                    }
                }
            }
        }


        @ParameterizedTest
        @ArgumentsSource(CombinationArgumentsProvider::class)
        fun `should resolve dependency`(apiVersion: ChartApiVersion, yamlStyle: YamlStyle) {

            setupProjectFiles(
                apiVersion, yamlStyle,
                mapOf(
                    "name" to "foo",
                    "version" to "*"
                )
            )

            directory("$projectDir/bar") {
                file(
                    "build.gradle", append = true,
                    contents = """
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

            val filteredRequirementsFile =
                projectDir.resolve("bar/build/helm/charts/bar/${apiVersion.requirementsFileName}")
            assertThat(filteredRequirementsFile, apiVersion.requirementsFileName)
                .containsDependency(
                    name = "foo", version = "1.2.3",
                    repository = "file://../../../../../foo/build/helm/charts/foo", index = 0
                )
        }


        @ParameterizedTest
        @ArgumentsSource(CombinationArgumentsProvider::class)
        fun `should resolve dependency and add version if not present`(apiVersion: ChartApiVersion, yamlStyle: YamlStyle) {

            setupProjectFiles(
                apiVersion, yamlStyle,
                mapOf("name" to "foo")
            )

            directory("$projectDir/bar") {
                file(
                    "build.gradle", append = true,
                    contents = """
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

            val filteredRequirementsFile =
                projectDir.resolve("bar/build/helm/charts/bar/${apiVersion.requirementsFileName}")
            assertThat(filteredRequirementsFile, apiVersion.requirementsFileName)
                .containsDependency(
                    name = "foo", version = "1.2.3",
                    repository = "file://../../../../../foo/build/helm/charts/foo", index = 0
                )
        }


        @ParameterizedTest
        @ArgumentsSource(CombinationArgumentsProvider::class)
        fun `should resolve dependency using alias`(apiVersion: ChartApiVersion, yamlStyle: YamlStyle) {

            setupProjectFiles(
                apiVersion, yamlStyle,
                mapOf(
                    "name" to "foo",
                    "version" to "*",
                    "alias" to "fooAlias"
                )
            )

            directory("$projectDir/bar") {
                file(
                    "build.gradle", append = true,
                    contents = """
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

            val filteredRequirementsFile =
                projectDir.resolve("bar/build/helm/charts/bar/${apiVersion.requirementsFileName}")
            assertThat(filteredRequirementsFile, apiVersion.requirementsFileName)
                .containsDependency(
                    name = "foo", version = "1.2.3",
                    repository = "file://../../../../../foo/build/helm/charts/foo", alias = "fooAlias", index = 0
                )
        }
    }


    private fun Assert<File>.containsDependency(
        name: String, version: String, repository: String, alias: String? = null, index: Int = 0
    ) = yamlContents().all {
        at("$.dependencies[$index].name")
            .hasValueEqualTo(name)
        at("$.dependencies[$index].version")
            .hasValueEqualTo(version)
        at("$.dependencies[$index].repository")
            .hasValueEqualTo(repository)
        at("$.dependencies[$index].alias")
            .run { if (alias != null) hasValueEqualTo(alias) else isNotPresent() }
    }
}
