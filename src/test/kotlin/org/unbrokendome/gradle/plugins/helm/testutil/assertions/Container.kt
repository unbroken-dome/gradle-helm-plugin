package org.unbrokendome.gradle.plugins.helm.testutil.assertions

import assertk.Assert
import assertk.assertions.support.expected
import assertk.assertions.support.show
import org.gradle.api.NamedDomainObjectCollection


fun <T : Any> Assert<NamedDomainObjectCollection<T>>.containsItem(name: String) =
    transform { actual ->
        actual.findByName(name) ?: expected("to contain an item named \"$name\"")
    }


fun <T : Any> Assert<NamedDomainObjectCollection<T>>.doesNotContainItem(name: String) = given { actual ->
    val item = actual.findByName(name)
    if (item != null) {
        expected("to contain no item named \"$name\", but did contain: ${show(item)}")
    }
}
