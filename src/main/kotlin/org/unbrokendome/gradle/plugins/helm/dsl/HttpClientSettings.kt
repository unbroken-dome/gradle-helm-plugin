package org.unbrokendome.gradle.plugins.helm.dsl

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.unbrokendome.gradle.plugins.helm.util.listProperty
import javax.inject.Inject


/**
 * DSL for configuring the HTTP client for various purposes.
 *
 * Currently only the version of the dependency can be configured.
 */
interface HttpClientSettings {

    /**
     * The external module dependencies for the HTTP client library. This must resolve to an implementation
     * compatible with the [OkHttp 4.5](https://hc.apache.org/httpcomponents-client-5.0.x/index.html).
     */
    val artifacts: ListProperty<String>
}


internal interface HttpClientSettingsInternal : HttpClientSettings {

    /**
     * Returns a [FileCollection] that contains the HTTP client library artifacts and their transitive dependencies.
     *
     * @param project the [Project] in which to resolve the dependencies
     * @return the dependencies as a [FileCollection]
     */
    fun dependenciesAsFileCollection(project: Project): FileCollection
}


private val DEFAULT_HTTPCLIENT_ARTIFACTS = listOf(
    "com.squareup.okhttp3:okhttp:4.5.0",
    "com.squareup.okhttp3:okhttp-tls:4.5.0"
)


private open class DefaultHttpClientSettings
@Inject constructor(
    objects: ObjectFactory
) : HttpClientSettings, HttpClientSettingsInternal {

    override val artifacts: ListProperty<String> =
        objects.listProperty<String>()
            .convention(DEFAULT_HTTPCLIENT_ARTIFACTS)


    override fun dependenciesAsFileCollection(project: Project): FileCollection {
        val dependencies = artifacts.get().map {
            project.dependencies.create(it)
        }
        return project.configurations.detachedConfiguration(*dependencies.toTypedArray())
    }
}


internal fun ObjectFactory.newHttpClientSettings(): HttpClientSettings =
    newInstance(DefaultHttpClientSettings::class.java)
