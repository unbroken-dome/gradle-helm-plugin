package org.unbrokendome.gradle.plugins.helm.dsl.credentials

import org.gradle.api.credentials.Credentials
import org.gradle.api.file.RegularFileProperty


/**
 * Credentials that identify the client by means of a TLS certificate.
 */
interface CertificateCredentials : Credentials {

    /**
     * Path to the certificate file (PEM format).
     */
    val certificateFile: RegularFileProperty

    /**
     * Path to the certificate private key file (PEM format).
     */
    val keyFile: RegularFileProperty
}
