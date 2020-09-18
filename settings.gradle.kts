pluginManagement {

    repositories {
        gradlePluginPortal()
        jcenter()
    }

    val kotlinVersion: String by settings
    resolutionStrategy.eachPlugin {
        if (requested.id.namespace == "org.jetbrains.kotlin" ||
                requested.id.namespace.orEmpty().startsWith("org.jetbrains.kotlin.")) {
            useVersion(kotlinVersion)
        }
    }
}


rootProject.name = "gradle-helm-plugin"
