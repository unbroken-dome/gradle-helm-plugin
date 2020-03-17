package org.unbrokendome.gradle.plugins.helm.rules

import org.gradle.api.Rule


/**
 * Abstract base class for all domain object rules.
 *
 * This class simply implements [toString] based on the [description][getDescription] of the rule.
 */
abstract class AbstractRule : Rule {

    override fun toString(): String =
        "Rule: $description"
}
