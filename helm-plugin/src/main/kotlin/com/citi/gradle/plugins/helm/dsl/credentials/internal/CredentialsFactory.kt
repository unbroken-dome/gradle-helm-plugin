package com.citi.gradle.plugins.helm.dsl.credentials.internal

import org.gradle.api.credentials.Credentials
import org.gradle.api.model.ObjectFactory
import com.citi.gradle.plugins.helm.dsl.credentials.CertificateCredentials
import com.citi.gradle.plugins.helm.dsl.credentials.DefaultCertificateCredentials
import com.citi.gradle.plugins.helm.dsl.credentials.DefaultPasswordCredentials
import com.citi.gradle.plugins.helm.dsl.credentials.PasswordCredentials


/**
 * Instantiates [Credentials] objects of known types.
 */
interface CredentialsFactory {

    /**
     * Instantiates a credentials object given its public type.
     *
     * @param type the public type of the credentials
     * @return the new credentials object, implementing `T`
     * @throws IllegalArgumentException if `type` is not a supported credentials type
     */
    fun <T : Credentials> create(type: Class<T>): T

    /**
     * Gets the public type for the given credentials instance.
     *
     * @param credentials a [Credentials] instance
     * @return the public credentials type
     * @throws NoSuchElementException if the given credentials' public type is not known (should never happen
     *         if `credentials` was constructed by this factory)
     */
    fun getPublicType(credentials: Credentials): Class<out Credentials>
}


class DefaultCredentialsFactory(private val objectFactory: ObjectFactory) : CredentialsFactory {

    private val knownTypes: Map<Class<out Credentials>, Class<out Credentials>> = mapOf(
        PasswordCredentials::class.java to DefaultPasswordCredentials::class.java,
        CertificateCredentials::class.java to DefaultCertificateCredentials::class.java
    )


    override fun <T : Credentials> create(type: Class<T>): T {
        val implementationType = knownTypes[type]
            ?: throw IllegalArgumentException("Unsupported credentials type: ${type.name}. " +
                    "The following types are supported: ${knownTypes.keys.joinToString(", ") { it.name }}"
            )
        return objectFactory.newInstance(implementationType)
            .let { type.cast(it) }
    }


    override fun getPublicType(credentials: Credentials): Class<out Credentials> {
        return knownTypes.keys
            .first { it.isInstance(credentials) }
    }
}
