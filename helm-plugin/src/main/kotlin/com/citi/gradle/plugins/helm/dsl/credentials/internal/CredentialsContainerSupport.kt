package com.citi.gradle.plugins.helm.dsl.credentials.internal

import org.gradle.api.Action
import org.gradle.api.credentials.Credentials
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import com.citi.gradle.plugins.helm.dsl.credentials.CredentialsContainer
import com.citi.gradle.plugins.helm.dsl.credentials.PasswordCredentials
import org.unbrokendome.gradle.pluginutils.property


/**
 * Support for implementing [CredentialsContainer].
 */
class CredentialsContainerSupport(
    objectFactory: ObjectFactory,
    private val credentialsFactory: CredentialsFactory
) : CredentialsContainer {

    private val credentials: Property<Credentials> =
        objectFactory.property()


    override fun getCredentials(): PasswordCredentials {
        return (credentials.orNull ?: setCredentials(PasswordCredentials::class.java))
                as? PasswordCredentials
            ?: throw IllegalStateException(
                "Can not use getCredentials() method when not using " +
                        "PasswordCredentials; please use getCredentials(Class)"
            )
    }


    override fun <T : Credentials> getCredentials(type: Class<T>): T {
        return (credentials.orNull ?: setCredentials(type))
            .let {
                require(type.isInstance(it)) {
                    "Given credentials type '${type.name}' does not match actual type " +
                            "'${credentialsFactory.getPublicType(it).name}'"
                }
                type.cast(it)
            }
    }


    private fun <T : Credentials> setCredentials(type: Class<T>): T {
        return credentialsFactory.create(type)
            .also { credentials.set(it) }
    }


    override fun credentials(configAction: Action<in PasswordCredentials>) {
        require(!credentials.isPresent || credentials.get() is PasswordCredentials) {
            "Cannot use credentials(Action) method when not using PasswordCredentials; " +
                    "please use credentials(Class, Action)"
        }
        credentials(PasswordCredentials::class.java, configAction)
    }


    override fun <T : Credentials> credentials(type: Class<T>, configAction: Action<in T>) {
        configAction.execute(getCredentials(type))
    }


    override val configuredCredentials: Provider<Credentials>
        get() = credentials
}
