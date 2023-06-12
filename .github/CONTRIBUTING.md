# Local builds

## Preconditions

* Java 8 or newer

## Running local builds

To run local builds, execute `./gradlew build` which will download all dependencies and then will compile and test the
project.

The project uses [Dokka](https://github.com/Kotlin/dokka) for documentation generation (analogue of Javadoc on Kotlin
world). This process might take some time, however if you'd like to disable it please specify the following property in
your user `gradle.properties` file: `com.citi.gradle.helm.plugin.dokka.disabled=true`.

