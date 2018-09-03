package org.unbrokendome.gradle.plugins.helm.dsl

import assertk.assert
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import org.gradle.api.file.RegularFile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.unbrokendome.gradle.plugins.helm.AbstractGradleProjectTest
import org.unbrokendome.gradle.plugins.helm.HelmPlugin
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.CertificateCredentials
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.CredentialsContainer
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.PasswordCredentials
import org.unbrokendome.gradle.plugins.helm.dsl.credentials.credentials
import org.unbrokendome.gradle.plugins.helm.testutil.hasValueEqualTo
import org.unbrokendome.gradle.plugins.helm.testutil.isInstanceOf
import org.unbrokendome.gradle.plugins.helm.testutil.isPresent
import java.io.File


@Suppress("NestedLambdaShadowedImplicitParameter")
class HelmRepositoryCredentialsTest : AbstractGradleProjectTest() {

    @BeforeEach
    fun applyPlugin() {
        project.plugins.apply(HelmPlugin::class.java)
    }


    @Test
    fun `Repository with password credentials`() {
        with (project.helm.repositories) {
            create("myRepo") { repo ->
                repo.url.set(project.uri("http://repository.example.com"))
                repo.credentials {
                    username.set("username")
                    password.set("password")
                }
            }
        }

        val repository = project.helm.repositories.getByName("myRepo")
        assert(repository, name = "repository")
                .prop(CredentialsContainer::configuredCredentials)
                .isPresent {
                    it.isInstanceOf(PasswordCredentials::class) {
                        it.prop(PasswordCredentials::username).hasValueEqualTo("username")
                        it.prop(PasswordCredentials::password).hasValueEqualTo("password")
                    }
                }
    }


    @Test
    fun `Repository with certificate credentials`() {
        with (project.helm.repositories) {
            create("myRepo") { repo ->
                repo.url.set(project.uri("http://repository.example.com"))
                repo.credentials(CertificateCredentials::class) {
                    certificateFile.set(project.file("/path/to/certificate"))
                    keyFile.set(project.file("/path/to/key"))
                }
            }
        }

        val repository = project.helm.repositories.getByName("myRepo")
        assert(repository, name = "repository")
                .prop(CredentialsContainer::configuredCredentials)
                .isPresent {
                    it.isInstanceOf(CertificateCredentials::class) {
                        it.prop(CertificateCredentials::certificateFile)
                                .isPresent {
                                    it.prop("asFile", RegularFile::getAsFile)
                                            .isEqualTo(File("/path/to/certificate"))
                                }
                        it.prop(CertificateCredentials::keyFile)
                                .isPresent {
                                    it.prop("asFile", RegularFile::getAsFile)
                                            .isEqualTo(File("/path/to/key"))
                                }
                    }
                }
    }
}
