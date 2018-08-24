package org.unbrokendome.gradle.plugins.helm

import assertk.all
import assertk.assert
import assertk.assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmAddRepository
import org.unbrokendome.gradle.plugins.helm.dsl.helm
import org.unbrokendome.gradle.plugins.helm.dsl.repositories
import org.unbrokendome.gradle.plugins.helm.testutil.hasValueEqualTo
import org.unbrokendome.gradle.plugins.helm.testutil.isInstanceOf
import java.net.URI


@Suppress("NestedLambdaShadowedImplicitParameter")
class HelmPluginRepositoriesTest : AbstractGradleProjectTest() {

    @BeforeEach
    fun applyPlugin() {
        project.plugins.apply(HelmPlugin::class.java)
    }


    @Test
    fun `Plugin should create a HelmAddRepository task for each repository`() {
        with (project.helm.repositories) {
            create("myRepo") { repo ->
                repo.url.set(project.uri("http://repository.example.com"))
            }
        }

        evaluateProject()

        val addRepoTask = project.tasks.findByName("helmAddMyRepoRepository")
        assert(addRepoTask, name = "add repository task")
                .isInstanceOf(HelmAddRepository::class) {
                    it.prop(HelmAddRepository::url).hasValueEqualTo(URI("http://repository.example.com"))
                }
    }


    @Test
    fun `Plugin should create a helmAddRepositories task that registers all repos`() {
        with (project.helm.repositories) {
            create("myRepo1") { repo ->
                repo.url.set(project.uri("http://repository1.example.com"))
            }
            create("myRepo2") { repo ->
                repo.url.set(project.uri("http://repository2.example.com"))
            }
        }

        evaluateProject()

        val addRepositoriesTask = project.tasks.findByName("helmAddRepositories")
        assert(addRepositoriesTask, name = "add repositories task")
                .isNotNull {
                    it.prop("dependencies") { it.taskDependencies.getDependencies(it) }
                            .all {
                                hasSize(2)
                                each { it.isInstanceOf(HelmAddRepository::class) }
                            }
                }

    }
}
