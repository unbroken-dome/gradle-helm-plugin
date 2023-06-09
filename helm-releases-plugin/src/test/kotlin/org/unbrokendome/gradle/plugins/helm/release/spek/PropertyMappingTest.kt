package com.citi.gradle.plugins.helm.release.spek

import assertk.assertThat
import assertk.assertions.prop
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.spekframework.spek2.style.specification.Suite
import org.unbrokendome.gradle.pluginutils.asFile
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.containsTask
import org.unbrokendome.gradle.pluginutils.test.assertions.assertk.hasValueEqualTo
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1


class PropertyMappingInfo<in D : Any, T : Task, V : Any>(
    private val propertyName: String,
    private val domainObjectValueSetter: D.(V) -> Unit,
    private val taskValueGetter: T.() -> Provider<V>,
    private val sampleValueSupplier: Project.() -> V
) {

    fun createTest(
        suite: Suite,
        domainObjectSupplier: () -> D,
        taskName: String,
        taskType: KClass<T>
    ) {
        suite.it("should use $propertyName property") {
            val project: Project by memoized()
            val domainObject = domainObjectSupplier()

            val sampleValue = sampleValueSupplier(project)

            domainObjectValueSetter.invoke(domainObject, sampleValue)

            assertThat(project, name = "project")
                .containsTask(taskName, taskType)
                .prop(propertyName, taskValueGetter)
                .hasValueEqualTo(sampleValue)
        }
    }
}


fun <D : Any, T : Task, V : Any> propertyMappingInfo(
    propertyName: String,
    domainObjectValueSetter: D.(V) -> Unit,
    taskValueGetter: T.() -> Provider<V>,
    sampleValue: V
) =
    PropertyMappingInfo(propertyName, domainObjectValueSetter, taskValueGetter, { sampleValue })


fun <D : Any, T : Task, V : Any> propertyMappingInfo(
    domainObjectValueSetter: D.(V) -> Unit,
    taskProperty: KProperty1<T, Provider<V>>,
    sampleValue: V
): PropertyMappingInfo<D, T, V> =
    propertyMappingInfo(
        taskProperty.name, domainObjectValueSetter, { taskProperty.get(this) }, sampleValue
    )


fun <D : Any, T : Task, V : Any> propertyMappingInfo(
    domainObjectProperty: KProperty1<D, Property<V>>,
    taskProperty: KProperty1<T, Provider<V>>,
    sampleValue: V
): PropertyMappingInfo<D, T, V> =
    propertyMappingInfo(
        { domainObjectProperty.get(this).set(it) }, taskProperty, sampleValue
    )


fun <D : Any, T : Task> propertyMappingInfo(
    domainObjectProperty: KProperty1<D, RegularFileProperty>,
    taskProperty: KProperty1<T, Provider<RegularFile>>,
    sampleFileName: String
) =
    PropertyMappingInfo<D, T, File>(
        taskProperty.name,
        { domainObjectProperty(this).set(it) },
        { taskProperty.get(this).asFile() },
        { file(sampleFileName) }
    )


class PropertyMappingTests<D : Any, T : Task>(
    private val domainObjectSupplier: () -> D,
    private val taskName: String,
    private val taskType: KClass<T>,
    vararg val properties: PropertyMappingInfo<D, T, *>
) : (Suite) -> Unit {

    override fun invoke(suite: Suite) {
        for (property in properties) {
            property.createTest(suite, domainObjectSupplier, taskName, taskType)
        }
    }
}


fun <D : Any, T : Task> Suite.propertyMappingTests(
    domainObjectSupplier: () -> D,
    taskName: String,
    taskType: KClass<T>,
    vararg properties: PropertyMappingInfo<D, T, *>
) {
    PropertyMappingTests(domainObjectSupplier, taskName, taskType, *properties)(this)
}


inline fun <D : Any, reified T : Task> Suite.propertyMappingTests(
    noinline domainObjectSupplier: () -> D,
    taskName: String,
    vararg properties: PropertyMappingInfo<D, T, *>
) = propertyMappingTests(domainObjectSupplier, taskName, T::class, *properties)
