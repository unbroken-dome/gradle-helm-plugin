package org.unbrokendome.gradle.plugins.helm.testutil.assertions

import assertk.Assert
import assertk.assertions.support.expected
import assertk.assertions.support.show
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.PathNotFoundException
import com.jayway.jsonpath.TypeRef
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import java.io.File
import java.nio.charset.Charset
import java.util.Optional
import kotlin.reflect.KClass


private val yamlMapper: YAMLMapper by lazy { YAMLMapper() }


private fun yamlJsonPathConfiguration(options: Set<Option>): Configuration =
    Configuration.builder()
        .jsonProvider(JacksonJsonProvider(yamlMapper))
        .mappingProvider(JacksonMappingProvider(yamlMapper))
        .options(options)
        .build()


fun Assert<File>.yamlContents(charset: Charset = Charsets.UTF_8, vararg options: Option): Assert<DocumentContext> =
    transform("$name (YAML)") { actual ->
        actual.inputStream().use { input ->
            JsonPath.using(yamlJsonPathConfiguration(options.toSet()))
                .parse(input, charset.name())
        }
    }


fun Assert<CharSequence>.asYaml(vararg options: Option): Assert<DocumentContext> =
    transform("$name (YAML)") { actual ->
        JsonPath.using(yamlJsonPathConfiguration(options.toSet()))
            .parse(actual.toString())
    }


class JsonPathWrapper(
    val context: DocumentContext,
    val path: JsonPath
) {

    fun <T : Any> evaluate(type: KClass<T>): T? =
        context.read(path, type.java)

    fun <T : Any> evaluate(typeRef: TypeRef<T>): T? =
        context.read(path, typeRef)

    inline fun <reified T : Any> evaluate(): T? =
        evaluate(object : TypeRef<T>() {})
}


fun Assert<DocumentContext>.at(path: String) = transform { actual ->
    JsonPathWrapper(context = actual, path = JsonPath.compile(path))
}


fun Assert<JsonPathWrapper>.hasValueEqualTo(value: String) = given { actual ->
    try {
        val actualValue = actual.evaluate<String>()
        if (actualValue == value) return
        expected(
            "to have an entry at ${show(actual.path.path)} equal to ${show(value)}, but was ${show(actualValue)}",
            actual = actual.context.jsonString()
        )

    } catch (e: PathNotFoundException) {
        expected(
            "to have an entry at ${show(actual.path.path)} equal to ${show(value)}, but path was not present",
            actual = actual.context.jsonString()
        )
    }
}


fun Assert<JsonPathWrapper>.isNotPresent() = given { actual ->
    try {
        val actualValue = actual.evaluate<Any>()
        expected(
            "to have no entry at ${show(actual.path.path)}, but was ${show(actualValue)}",
            actual = actual.context.jsonString()
        )
    } catch (e: PathNotFoundException) {
        // ok
    }
}


val <T : Any> Assert<Optional<T>>.value: Assert<T>
    get() = transform { actual ->
        if (actual.isEmpty) {
            expected("to have a value")
        } else actual.get()
    }


fun <T : Any> Assert<Optional<T>>.isEmpty() = given { actual ->
    if (actual.isEmpty) return
    expected("to be empty")
}
