package org.unbrokendome.gradle.plugins.helm.dsl.credentials

import org.gradle.api.Action
import org.gradle.api.credentials.Credentials


/**
 * Support for implementing [CredentialsContainer].
 */
internal class CredentialsContainerSupport(private val credentialsFactory: CredentialsFactory)
    : CredentialsContainer {

    private var credentials: Credentials? = null


    override fun getCredentials(): PasswordCredentials {
        return (credentials ?: setCredentials(PasswordCredentials::class.java))
                as? PasswordCredentials
                ?: throw IllegalStateException("Can not use getCredentials() method when not using " +
                        "PasswordCredentials; please use getCredentials(Class)")
    }


    override fun <T : Credentials> getCredentials(type: Class<T>): T {
        return (credentials ?: setCredentials(type))
                .let {
                    require(type.isInstance(it)) { "Given credentials type '${type.name}' does not match actual type " +
                            "'${credentialsFactory.getPublicType(it).name}'" }
                    type.cast(it)
                }
    }


    private fun <T : Credentials> setCredentials(type: Class<T>): T {
        return credentialsFactory.create(type)
                .also { credentials = it }
    }


    override fun credentials(configAction: Action<in PasswordCredentials>) {
        require(credentials == null || credentials is PasswordCredentials) {
            "Cannot use credentials(Action) method when not using PasswordCredentials; " +
                    "please use credentials(Class, Action)"
        }
        credentials(PasswordCredentials::class.java, configAction)
    }


    override fun <T : Credentials> credentials(type: Class<T>, configAction: Action<in T>) {
        configAction.execute(getCredentials(type))
    }


    override val configuredCredentials: Credentials?
        get() = credentials
}
