plugins {
    kotlin("jvm")
    alias(libs.plugins.detekt)
}

dependencies {
    api(gradleTestKit())
    api(libs.kotestAssertions)
    api(libs.junit)
    runtimeOnly(libs.junitEngine)
}