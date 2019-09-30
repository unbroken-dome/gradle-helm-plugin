package org.unbrokendome.gradle.plugins.helm

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import org.gradle.api.Task
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.unbrokendome.gradle.plugins.helm.command.tasks.HelmAddRepository
import org.unbrokendome.gradle.plugins.helm.dsl.helm
import org.unbrokendome.gradle.plugins.helm.dsl.repositories
import org.unbrokendome.gradle.plugins.helm.testutil.assertions.containsTask
import org.unbrokendome.gradle.plugins.helm.testutil.assertions.isPresent
import org.unbrokendome.gradle.plugins.helm.testutil.assertions.taskDependencies
import java.net.URI


class HelmPluginRepositoriesTest : AbstractGradleProjectTest() {

    @BeforeEach
    fun applyPlugin() {
        project.plugins.apply(HelmPlugin::class.java)
    }


    @Test
    fun `Plugin should create a HelmAddRepository task for each repository`() {
        with(project.helm.repositories) {
            create("myRepo") { repo ->
                repo.url.set(project.uri("http://repository.example.com"))
            }
        }

        evaluateProject()

        assertThat(project)
            .containsTask<HelmAddRepository>("helmAddMyRepoRepository")
            .prop(HelmAddRepository::url)
            .isPresent().isEqualTo(URI("http://repository.example.com"))
    }


    @Test
    fun `Plugin should create a helmAddRepositories task that registers all repos`() {
        with(project.helm.repositories) {
            create("myRepo1") { repo ->
                repo.url.set(project.uri("http://repository1.example.com"))
            }
            create("myRepo2") { repo ->
                repo.url.set(project.uri("http://repository2.example.com"))
            }
        }

        evaluateProject()

        assertThat(project)
            .containsTask<Task>("helmAddRepositories")
            .taskDependencies.all {
                each { it.isInstanceOf(HelmAddRepository::class) }
                extracting { it.name }.containsOnly("helmAddMyRepo1Repository", "helmAddMyRepo2Repository")
            }
    }
}
