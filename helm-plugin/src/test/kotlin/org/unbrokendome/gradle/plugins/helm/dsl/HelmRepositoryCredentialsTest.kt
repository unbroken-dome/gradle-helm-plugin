package com.citi.gradle.plugins.helm.dsl

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import com.citi.gradle.plugins.helm.HelmPlugin
import com.citi.gradle.plugins.helm.dsl.credentials.CertificateCredentials
import com.citi.gradle.plugins.helm.dsl.credentials.CredentialsContainer
import com.citi.gradle.plugins.helm.dsl.credentials.PasswordCredentials
import com.citi.gradle.plugins.helm.dsl.credentials.credentials
import com.citi.gradle.plugins.helm.dsl.internal.helm
import com.citi.gradle.plugins.helm.dsl.internal.repositories
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.containsItem
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.fileValue
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.isPresent
import org.unbrokendome.gradle.pluginutils.test.spek.applyPlugin
import org.unbrokendome.gradle.pluginutils.test.spek.setupGradleProject
import java.io.File


object HelmRepositoryCredentialsTest : Spek({

    val project by setupGradleProject { applyPlugin<HelmPlugin>() }


    describe("repository with password credentials") {

        beforeEachTest {
            with(project.helm.repositories) {
                create("myRepo") { repo ->
                    repo.url.set(project.uri("http://repository.example.com"))
                    repo.credentials { cred ->
                        cred.username.set("username")
                        cred.password.set("password")
                    }
                }
            }
        }

        it("should create a PasswordCredentials object") {
            assertThat(project.helm.repositories, name = "repositories")
                .containsItem("myRepo")
                .prop(CredentialsContainer::configuredCredentials)
                .isPresent().isInstanceOf(PasswordCredentials::class)
                .all {
                    prop(PasswordCredentials::username).isPresent().isEqualTo("username")
                    prop(PasswordCredentials::password).isPresent().isEqualTo("password")
                }
        }
    }


    describe("repository with certificate credentials") {

        beforeEachTest {
            with(project.helm.repositories) {
                create("myRepo") { repo ->
                    repo.url.set(project.uri("http://repository.example.com"))
                    repo.credentials(CertificateCredentials::class) {
                        certificateFile.set(project.file("/path/to/certificate"))
                        keyFile.set(project.file("/path/to/key"))
                    }
                }
            }
        }

        it("should create a CertificateCredentials object") {
            assertThat(project.helm.repositories, name = "repositories")
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
})
