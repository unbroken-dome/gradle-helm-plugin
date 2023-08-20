pluginManagement {

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    resolutionStrategy.eachPlugin {
        if (requested.id.namespace == "org.jetbrains.kotlin" ||
                requested.id.namespace.orEmpty().startsWith("org.jetbrains.kotlin.")) {
            useVersion(embeddedKotlinVersion)
        }
    }
}


dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}


rootProject.name = "gradle-helm-plugin-parent"

include(
    "helm-plugin",
    "helm-publish-plugin",
    "helm-releases-plugin"
)
