package com.citi.gradle.plugins.helm.command

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.workers.WorkParameters


internal interface HelmExecWorkParameters : WorkParameters {

    val executable: Property<String>

    val args: ListProperty<String>

    val environment: MapProperty<String, Any>

    val ignoreExitValue: Property<Boolean>

    val stdoutFile: RegularFileProperty
}


internal class WorkParametersHelmExecSpec(
    private val params: HelmExecWorkParameters
) : HelmExecSpec {

    override fun executable(executable: String) {
        params.executable.set(executable)
    }


    override fun args(args: Iterable<Any?>) {
        params.args.addAll(args.map { it.toString() })
    }


    override fun environment(name: String, provider: Provider<out Any>) {
        // Eagerly evaluate the provider here, to work around a bug in Gradle that makes
        // the whole MapProperty "missing" if the value provider is missing
        // see https://github.com/gradle/gradle/issues/13364
        provider.orNull?.let { value ->
            params.environment.put(name, value)
        }
    }


    override fun assertSuccess(assertSuccess: Boolean) {
        params.ignoreExitValue.set(!assertSuccess)
    }
}
