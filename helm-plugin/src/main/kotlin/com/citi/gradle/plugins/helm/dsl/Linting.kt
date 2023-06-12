package com.citi.gradle.plugins.helm.dsl

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import com.citi.gradle.plugins.helm.command.ConfigurableHelmValueOptions
import com.citi.gradle.plugins.helm.command.internal.mergeValues
import javax.inject.Inject


/**
 * Defines options for linting Helm charts using the `helm lint` command.
 */
interface Linting : ConfigurableHelmValueOptions {

    /**
     * A configuration of values to pass to `helm lint`.
     */
    interface Configuration : Named, ConfigurableHelmValueOptions {

        companion object {
            @JvmStatic
            val DEFAULT_CONFIGURATION_NAME = "default"
        }
    }


    /**
     * If `true` (the default), run the linter.
     */
    val enabled: Property<Boolean>

    /**
     * If `true`, treat warnings from the linter as errors.
     *
     * Corresponds to the `--strict` CLI option.
     */
    val strict: Property<Boolean>

    /**
     * If `true`, also lint dependent charts.
     *
     * Corresponds to the `--with-subcharts` CLI option.
     */
    val withSubcharts: Property<Boolean>

    /**
     * A collection of linter configurations.
     *
     * Each configuration allows specifying a different set of values to pass to `helm lint`. Values defined in
     * the `Linting` block directly are merged into all configurations.
     *
     * If no configurations are added to this container, the plugin will assume a single configuration named
     * "default" with no additional values.
     */
    val configurations: NamedDomainObjectContainer<Configuration>


    /**
     * Configures the linter configurations.
     *
     * Each configuration allows specifying a different set of values to pass to `helm lint`. Values defined in
     * the `Linting` block directly are merged into all configurations.
     *
     * If no configurations are added to this container, the plugin will assume a single configuration named
     * "default" with no additional values.
     */
    fun configurations(configureAction: Action<NamedDomainObjectContainer<Configuration>>) =
        configurations.also { configureAction.execute(it) }
}


internal fun Linting.setParent(parent: Linting) {
    mergeValues(parent)
    enabled.set(parent.enabled)
    strict.set(parent.strict)
    withSubcharts.set(parent.withSubcharts)
}


// Unfortunately Gradle isn't smart enough (yet) to implement the Named interface for us,
// otherwise we could just use the interfaces here
private abstract class DefaultLintConfiguration
@Inject constructor(
    private val name: String
) : Linting.Configuration {

    override fun getName(): String = name
}


private abstract class DefaultLinting
@Inject constructor(
    objects: ObjectFactory
) : Linting {

    final override val configurations: NamedDomainObjectContainer<Linting.Configuration> =
        objects.domainObjectContainer(Linting.Configuration::class.java) { name ->
            objects.newInstance(DefaultLintConfiguration::class.java, name)
        }
}


/**
 * Creates a new [Linting] object using the given [ObjectFactory].
 *
 * @receiver the Gradle [ObjectFactory] to create the object
 * @param parent the optional parent [Linting] object
 * @return the created [Linting] object
 */
internal fun ObjectFactory.createLinting(parent: Linting? = null): Linting =
    newInstance(DefaultLinting::class.java)
        .apply {
            enabled.convention(true)

            // Add a rule that creates a "default" lint configuration if the user didn't add any
            configurations.addRule("default lint configuration") { configurationName ->
                if (configurations.isEmpty() && configurationName == Linting.Configuration.DEFAULT_CONFIGURATION_NAME) {
                    configurations.create(Linting.Configuration.DEFAULT_CONFIGURATION_NAME)
                }
            }

            parent?.let { setParent(it) }
        }
