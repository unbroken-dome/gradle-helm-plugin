package org.unbrokendome.gradle.plugins.helm.testutil.assertions

import assertk.Assert
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.jayway.jsonpath.*
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.json.JsonProvider
import java.io.File
import java.nio.charset.Charset
import kotlin.reflect.KClass


private val yamlJsonPathJsonProvider: JsonProvider by lazy {
    JacksonJsonProvider(YAMLMapper())
}


private fun yamlJsonPathConfiguration(options: Set<Option>): Configuration =
    Configuration.builder()
        .jsonProvider(yamlJsonPathJsonProvider)
        .options(options)
        .build()


fun Assert<File>.yamlContents(charset: Charset = Charsets.UTF_8, vararg options: Option): Assert<DocumentContext> =
    transform("$name (YAML)") { actual ->
        JsonPath.using(yamlJsonPathConfiguration(options.toSet()))
            .let { parseContext ->
                actual.inputStream().use { input ->
                    parseContext.parse(input, charset.name())
                }
            }
    }


fun Assert<CharSequence>.asYaml(vararg options: Option): Assert<DocumentContext> =
    transform("$name (YAML)") { actual ->
        JsonPath.using(yamlJsonPathConfiguration(options.toSet()))
            .parse(actual.toString())
    }


fun <T : Any> Assert<DocumentContext>.jsonPath(path: String, type: KClass<T>, vararg filters: Predicate) =
    transform { actual ->
        actual.read(path, type.java, *filters)
    }


inline fun <reified T : Any> Assert<DocumentContext>.jsonPath(path: String, vararg filters: Predicate) =
    jsonPath(path, T::class, *filters)
