package org.unbrokendome.gradle.plugins.helm.dsl.credentials

import org.gradle.api.credentials.Credentials
import org.gradle.api.provider.Property


/**
 * Represents a set of username/password credentials.
 */
interface PasswordCredentials : Credentials {

    /**
     * The username.
     */
    val username: Property<String>

    /**
     * The password.
     */
    val password: Property<String>
}
