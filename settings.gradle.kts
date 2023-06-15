pluginManagement {

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}


rootProject.name = "gradle-helm-plugin-parent"

include(
    "helm-plugin",
    "helm-publish-plugin",
    "helm-releases-plugin",
    "plugin-test-utils"
)
