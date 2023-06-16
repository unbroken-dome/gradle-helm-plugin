plugins {
    kotlin("jvm")
    alias(libs.plugins.detekt)
}

dependencies {
    api(gradleTestKit())
    api(libs.bundles.defaultTests)
    runtimeOnly(libs.junitEngine)
}