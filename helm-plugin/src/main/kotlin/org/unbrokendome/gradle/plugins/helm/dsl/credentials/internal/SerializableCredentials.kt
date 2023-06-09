package com.citi.gradle.plugins.helm.dsl.credentials.internal

import org.gradle.api.credentials.Credentials
import com.citi.gradle.plugins.helm.dsl.credentials.CertificateCredentials
import com.citi.gradle.plugins.helm.dsl.credentials.PasswordCredentials
import java.io.File
import java.io.Serializable


sealed class SerializableCredentials : Serializable


class SerializablePasswordCredentials(
    val username: String,
    val password: String?
) : SerializableCredentials()


fun PasswordCredentials.toSerializable(): SerializablePasswordCredentials =
    SerializablePasswordCredentials(
        username = username.get(),
        password = password.orNull
    )


class SerializableCertificateCredentials(
    val certificateFile: File,
    val keyFile: File
) : SerializableCredentials()


fun CertificateCredentials.toSerializable(): SerializableCertificateCredentials =
    SerializableCertificateCredentials(
        certificateFile = certificateFile.get().asFile,
        keyFile = keyFile.get().asFile
    )


fun Credentials.toSerializable(): SerializableCredentials =
    when (this) {
        is PasswordCredentials ->
            this.toSerializable()
        is CertificateCredentials ->
            this.toSerializable()
        else ->
            throw IllegalStateException("Unsupported credentials type: ${javaClass.name}")
    }
