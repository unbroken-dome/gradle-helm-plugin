package com.citi.gradle.plugins.helm.dsl.credentials

import org.gradle.api.credentials.Credentials
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.unbrokendome.gradle.pluginutils.property
import javax.inject.Inject


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


internal open class DefaultPasswordCredentials
@Inject constructor(objectFactory: ObjectFactory) : PasswordCredentials {

    override val username: Property<String> =
        objectFactory.property()

    override val password: Property<String> =
        objectFactory.property()
}
