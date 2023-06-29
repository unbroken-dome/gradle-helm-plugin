# Local builds

## Preconditions

* Java 8 or newer

## Running local builds

To run local builds, execute `./gradlew build` which will download all dependencies and then will compile and test the
project.

The project uses [Dokka](https://github.com/Kotlin/dokka) for documentation generation (analogue of Javadoc on Kotlin
world). This process might take some time, however if you'd like to disable it please specify the following property in
your user `gradle.properties` file: `com.citi.gradle.helm.plugin.dokka.disabled=true`.

# Tests

Project has tests, based on two frameworks:

* [gradle-plugin-test-utils](https://github.com/unbroken-dome/gradle-plugin-utils), which
  use [Spek framework inside](https://github.com/spekframework/spek). Please note, that both projects aren't supported
  well (for example, [see this](https://github.com/spekframework/spek/issues/959#issuecomment-997344639))
* [JUnit](https://www.baeldung.com/kotlin/junit-5-kotlin) + [KoTest](https://github.com/kotest/kotest), which is used
  for new tests. JUnit 5 is used to run tests, KoTest is used **for assertions only** (because they are much more
  convenient via achieving Kotlin language advantages; however KoTest requires IntelliJ plugin to run single test, which
  might be inconvenient for developers).

Both frameworks use JUnit test runner, therefore `./gradlew test` covers all unit tests.

## Functional tests

Please read [Gradle advices first](https://docs.gradle.org/current/userguide/testing_gradle_plugins.html) about the test
writing. One of recommendations is to use separate source set and task to run functional tests, because unit tests
inherit classpath from plugin, which might be risky for functional tests.

Therefore, tests covering entire plugin (plus potential different gradle versions verification) are separated to
different source set and to different task. Please run `./gradlew functionalTest` task for that purpose.

Functional tests download distribution archives from `https://services.gradle.org/distributions`. If you'd like to use another web server (for example, corporate artifacts provider) - please configure gradle property `com.citi.gradle.helm.plugin.distribution.url.prefix` in `~/.gradle/gradle.properties`