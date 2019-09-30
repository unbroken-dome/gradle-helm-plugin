package org.unbrokendome.gradle.plugins.helm.dsl

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.gradle.api.NamedDomainObjectContainer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.unbrokendome.gradle.plugins.helm.AbstractGradleProjectTest
import org.unbrokendome.gradle.plugins.helm.HelmPlugin
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.CertificateCredentials
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.CredentialsContainer
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.PasswordCredentials
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.credentials
import org.unbrokendome.gradle.plugins.helm.testutil.assertions.containsItem
import org.unbrokendome.gradle.plugins.helm.testutil.assertions.fileValue
import org.unbrokendome.gradle.plugins.helm.testutil.assertions.isPresent
import java.io.File


class HelmRepositoryCredentialsTest : AbstractGradleProjectTest() {

    @BeforeEach
    fun applyPlugin() {
        project.plugins.apply(HelmPlugin::class.java)
    }


    val repositories: NamedDomainObjectContainer<HelmRepository>
        get() = project.helm.repositories


    @Test
    fun `Repository with password credentials`() {

        with(project.helm.repositories) {
            create("myRepo") { repo ->
                repo.url.set(project.uri("http://repository.example.com"))
                repo.credentials {
                    username.set("username")
                    password.set("password")
                }
            }
        }

        assertThat(this::repositories)
            .containsItem("myRepo")
            .prop(CredentialsContainer::configuredCredentials)
            .isPresent().isInstanceOf(PasswordCredentials::class)
            .all {
                prop(PasswordCredentials::username).isPresent().isEqualTo("username")
                prop(PasswordCredentials::password).isPresent().isEqualTo("password")
            }
    }


    @Test
    fun `Repository with certificate credentials`() {
        with(project.helm.repositories) {
            create("myRepo") { repo ->
                repo.url.set(project.uri("http://repository.example.com"))
                repo.credentials(CertificateCredentials::class) {
                    certificateFile.set(project.file("/path/to/certificate"))
                    keyFile.set(project.file("/path/to/key"))
                }
            }
        }

        assertThat(this::repositories)
            .containsItem("myRepo")
            .prop(CredentialsContainer::configuredCredentials)
            .isPresent().isInstanceOf(CertificateCredentials::class)
            .all {
                prop(CertificateCredentials::certificateFile).fileValue()
                    .isEqualTo(File("/path/to/certificate"))
                prop(CertificateCredentials::keyFile).fileValue()
                    .isEqualTo(File("/path/to/key"))
            }
    }
}
