pluginManagement {

    repositories {
        gradlePluginPortal()
        jcenter()
    }

    resolutionStrategy.eachPlugin {
        if (requested.id.namespace == "org.jetbrains.kotlin" ||
                requested.id.namespace.orEmpty().startsWith("org.jetbrains.kotlin.")) {
            useVersion(embeddedKotlinVersion)
        }
    }
}


rootProject.name = "gradle-helm-plugin"
