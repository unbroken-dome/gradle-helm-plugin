package org.unbrokendome.gradle.plugins.helm.command

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.putFrom
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
        params.environment.putFrom(name, provider)
    }


    override fun assertSuccess(assertSuccess: Boolean) {
        params.ignoreExitValue.set(!assertSuccess)
    }
}
