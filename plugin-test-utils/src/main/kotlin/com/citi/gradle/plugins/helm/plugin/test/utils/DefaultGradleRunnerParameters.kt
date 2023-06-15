package com.citi.gradle.plugins.helm.plugin.test.utils

import java.util.stream.Stream
import org.junit.jupiter.params.provider.Arguments

data class DefaultGradleRunnerParameters(override val distribution: GradleDistribution) : GradleRunnerParameters {
    companion object {
        @JvmStatic
        fun getDefaultParameterSet(): Stream<Arguments> {
            return GradleDistribution.all.map { gradleDistribution ->
                DefaultGradleRunnerParameters(gradleDistribution)
            }
                .map { Arguments.of(it) }
                .stream()
        }
    }
}
