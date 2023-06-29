package com.citi.gradle.plugins.helm.plugin.test.utils

sealed interface GradleDistribution {
    companion object {
        val all = buildList {
            add(Current)
            addAll(Custom.values())
        }
    }

    object Current : GradleDistribution

    /**
     * Ideally is to have couple the latest distributions from https://gradle.org/releases/.
     *
     * So we will be able to add even beta versions of Gradle in future.
     */
    enum class Custom(val version: String) : GradleDistribution {
        V8_1_1("8.1.1"),
        V7_6_1("7.6.1")
    }
}
