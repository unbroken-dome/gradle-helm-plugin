package org.unbrokendome.gradle.plugins.helm.dsl.credentials

import org.gradle.api.credentials.Credentials
import java.io.File
import java.io.Serializable


internal sealed class SerializableCredentials : Serializable


internal class SerializablePasswordCredentials(
    val username: String,
    val password: String?
) : SerializableCredentials()


internal fun PasswordCredentials.toSerializable(): SerializablePasswordCredentials =
    SerializablePasswordCredentials(
        username = username.get(),
        password = password.orNull
    )


internal class SerializableCertificateCredentials(
    val certificateFile: File,
    val keyFile: File
) : SerializableCredentials()


internal fun CertificateCredentials.toSerializable(): SerializableCertificateCredentials =
    SerializableCertificateCredentials(
        certificateFile = certificateFile.get().asFile,
        keyFile = keyFile.get().asFile
    )


internal fun Credentials.toSerializable(): SerializableCredentials =
    when (this) {
        is PasswordCredentials ->
            this.toSerializable()
        is CertificateCredentials ->
            this.toSerializable()
        else ->
            throw IllegalStateException("Unsupported credentials type: ${javaClass.name}")
    }
