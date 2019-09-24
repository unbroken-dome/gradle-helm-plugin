package org.unbrokendome.gradle.plugins.helm.rules

import org.gradle.api.Rule


abstract class AbstractRule : Rule {

    override fun toString(): String =
        "Rule: $description"
}
