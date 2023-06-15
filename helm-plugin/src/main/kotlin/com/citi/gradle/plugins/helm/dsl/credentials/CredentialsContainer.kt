package com.citi.gradle.plugins.helm.dsl.credentials

import com.citi.gradle.plugins.helm.dsl.credentials.internal.toAction
import groovy.lang.Closure
import groovy.lang.Closure.DELEGATE_FIRST
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.credentials.Credentials
import org.gradle.api.provider.Provider
import kotlin.reflect.KClass


/**
 * Holds credentials to access a remote service.
 *
 * Similar to the credentials part of Gradle's [org.gradle.api.artifacts.repositories.AuthenticationSupported]
 * (which unfortunately cannot be extended with new credential types).
 */
interface CredentialsContainer {

    /**
     * Returns the username/password credentials used to authenticate to this service.
     *
     * If no credentials have been assigned to this repository, an empty set of username/password credentials is
     * assigned to this repository and returned.
     *
     * If you are using a different type of credentials than [PasswordCredentials], please use
     * the `getCredentials(Class)` method to obtain the credentials.
     *
     * @return the current [PasswordCredentials]
     * @throws IllegalStateException if the credential type was previously set with [credentials] where the
     *         type was not [PasswordCredentials]
     */
    fun getCredentials(): PasswordCredentials


    /**
     * Returns the credentials of the specified type used to authenticate with this service.
     *
     * If no credentials have been assigned to this repository, an empty set of credentials of the specified type is
     * assigned to this repository and returned.
     *
     * @param type the type of credentials
     * @return the credentials
     * @throws IllegalArgumentException when the credentials assigned to this repository are not assignable to
     *         the specified type
     */
    fun <T : Credentials> getCredentials(type: Class<T>): T


    /**
     * Configures the credentials for this service using the supplied action.
     *
     * If no credentials have been assigned to this repository, an empty set of credentials of the specified type will
     * be assigned to this repository and given to the configuration action.
     *
     * If credentials have already been specified for this repository, they will be passed to the given
     * configuration action.
     *
     * ```
     * credentials(CertificateCredentials) {
     *     certificateFile file("/path/to/certificate")
     *     keyFile file("/path/to/key")
     * }
     * ```
     *
     * The following credential types are currently supported for the `type` argument:
     *
     * * [PasswordCredentials]
     * * [CertificateCredentials]
     *
     * @param type the type of credentials
     * @param configAction an [Action] to configure the credentials
     *
     * @throws IllegalArgumentException if `type` is not of a supported type
     * @throws IllegalArgumentException if `type` is of a different type to the credentials previously
     *         specified for this repository
     */
    fun <T : Credentials> credentials(type: Class<T>, configAction: Action<in T>)


    /**
     * Configures the credentials for this service using the supplied closure.
     *
     * If no credentials have been assigned to this repository, an empty set of credentials of the specified type will
     * be assigned to this repository and given to the configuration action.
     *
     * If credentials have already been specified for this repository, they will be passed to the given
     * configuration action.
     *
     * ```
     * credentials(CertificateCredentials) {
     *     certificateFile file("/path/to/certificate")
     *     keyFile file("/path/to/key")
     * }
     * ```
     *
     * The following credential types are currently supported for the `type` argument:
     *
     * * [PasswordCredentials]
     * * [CertificateCredentials]
     *
     * @param type the type of credentials
     * @param configClosure a [Closure] to configure the credentials
     *
     * @throws IllegalArgumentException if `type` is not of a supported type
     * @throws IllegalArgumentException if `type` is of a different type to the credentials previously
     *         specified for this repository
     */
    @Deprecated(
        message = "The function is deprecated: Gradle doesn't recommend to pass `Closure<*>` as input parameter. Nothing needs to changed for Groovy users, please use overloaded method in Java/Kotlin",
        replaceWith = ReplaceWith("credentials")
    )
    fun <T : Credentials> credentials(
        @DelegatesTo.Target type: Class<T>,
        @DelegatesTo(strategy = DELEGATE_FIRST, genericTypeIndex = 0) configClosure: Closure<*>
    ) {
        credentials(type, configClosure.toAction())
    }


    /**
     * Configures the username/password credentials for this service using the supplied action.
     *
     * If no credentials have been assigned to this repository, an empty set of username/password credentials is
     * assigned to this repository and passed to the action.
     *
     * ```
     * credentials {
     *     username = 'joe'
     *     password = 'secret'
     * }
     * ```
     *
     * @param configAction an [Action] to configure the credentials
     *
     * @throws IllegalStateException when the credentials assigned to this service are not of
     *         type [PasswordCredentials]
     */
    fun credentials(configAction: Action<in PasswordCredentials>)


    /**
     * Configures the username/password credentials for this service using the supplied closure.
     *
     * If no credentials have been assigned to this repository, an empty set of username/password credentials is
     * assigned to this repository and passed to the action.
     *
     * ```
     * credentials {
     *     username = 'joe'
     *     password = 'secret'
     * }
     * ```
     *
     * @param configClosure a [Closure] to configure the credentials
     *
     * @throws IllegalStateException when the credentials assigned to this service are not of
     *         type [PasswordCredentials]
     */
    @Deprecated(
        message = "The function is deprecated: Gradle doesn't recommend to pass `Closure<*>` as input parameter. Nothing needs to changed for Groovy users, please use overloaded method in Java/Kotlin",
        replaceWith = ReplaceWith("credentials")
    )
    fun credentials(
        @DelegatesTo(PasswordCredentials::class, strategy = DELEGATE_FIRST) configClosure: Closure<*>
    ) {
        credentials(configClosure.toAction())
    }


    /**
     * A provider that returns the currently configured credentials.
     *
     * Will have no value if no credentials have been configured.
     */
    val configuredCredentials: Provider<Credentials>
}


/**
 * Configures the username/password credentials for this service using the supplied action.
 *
 * Extension method for Kotlin support.
 *
 * @param configAction a lambda action to configure the credentials
 */
fun CredentialsContainer.credentials(configAction: PasswordCredentials.() -> Unit) {
    credentials(Action(configAction))
}


/**
 * Configures the credentials for this service using the supplied action.
 *
 * Extension method for Kotlin support.
 *
 * @param type the type of credentials
 * @param configAction the lambda action to configure the credentials
 */
fun <T : Credentials> CredentialsContainer.credentials(type: KClass<T>, configAction: T.() -> Unit) {
    credentials(type.java, Action(configAction))
}
