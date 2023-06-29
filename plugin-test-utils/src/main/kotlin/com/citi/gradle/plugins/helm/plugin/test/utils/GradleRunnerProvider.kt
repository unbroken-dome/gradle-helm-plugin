package com.citi.gradle.plugins.helm.plugin.test.utils

import java.net.URI
import org.gradle.testkit.runner.GradleRunner

object GradleRunnerProvider {
    private const val distributionUrlPrefixProperty = "com.citi.gradle.helm.plugin.distribution.url.prefix"
    private const val defaultDistributionUrlPrefix = "https://services.gradle.org/distributions"

    fun createRunner(parameters: GradleRunnerParameters): GradleRunner {
        return GradleRunner.create()
            .withPluginClasspath()
            .let { runner ->
                applyDistribution(runner, parameters.distribution)
            }
    }

    private fun applyDistribution(runner: GradleRunner, distribution: GradleDistribution): GradleRunner {
        return when (distribution) {
            GradleDistribution.Current -> runner
            is GradleDistribution.Custom -> applyCustomDistribution(runner, distribution)
        }
    }

    private fun applyCustomDistribution(runner: GradleRunner, distribution: GradleDistribution.Custom): GradleRunner {
        val distributionUrl = System.getProperty(distributionUrlPrefixProperty, defaultDistributionUrlPrefix)
        val url = "$distributionUrl/gradle-${distribution.version}-all.zip"

        return runner.withGradleDistribution(URI.create(url))
    }
}
