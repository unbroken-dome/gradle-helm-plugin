package com.citi.gradle.plugins.helm.release.tags

import org.gradle.api.provider.Provider


/**
 * Expression that matches on a set of tags.
 */
internal interface TagExpression {

    /**
     * Checks if this expression matches the given set of tags.
     *
     * @param tags the set of tags to check
     * @return `true` if the expression matches the set of tags, otherwise `false`
     */
    fun matches(tags: Set<String>): Boolean


    /**
     * Returns a tag expression that matches when both this and another given tag expression match.
     *
     * @param other another [TagExpression]
     * @return a [TagExpression] that is the AND-combination of this expression and [other]
     */
    fun and(other: TagExpression): TagExpression =
        when (other) {
            is AlwaysMatchTagExpression -> this
            else -> AndTagExpression(listOf(this, other))
        }


    /**
     * Returns a tag expression that matches when either this or another given tag expression matches (or both).
     *
     * @param other another [TagExpression]
     * @return a [TagExpression] that is the OR-combination of this expression and [other]
     */
    fun or(other: TagExpression): TagExpression =
        when (other) {
            is AlwaysMatchTagExpression -> other
            else -> OrTagExpression(listOf(this, other))
        }


    /**
     * Returns a tag expression that negates this expression (matches when this expression does not match, and
     * vice versa).
     *
     * @return a negated tag expression
     */
    fun not(): TagExpression =
        NotTagExpression(this)


    companion object {

        /**
         * Returns a tag expression that always matches, even if the set of tags is empty.
         *
         * @return an always-match [TagExpression]
         */
        fun alwaysMatch(): TagExpression =
            AlwaysMatchTagExpression

        /**
         * Returns a tag expression that matches if the set of tags contains a given tag.
         * @param tag the tag to check
         * @return the [TagExpression]
         */
        fun single(tag: String): TagExpression =
            SingleTagExpression(tag)

        /**
         * Returns a tag expression that matches if the set of tags contains any of a given set.
         *
         * @param tags the tags to check
         * @return the [TagExpression]
         */
        fun any(tags: Iterable<String>): TagExpression =
            AnyTagExpression(tags)

        /**
         * Returns a tag expression that matches if the set of tags contains all of a given set.
         *
         * @param tags the tags to check
         * @return the [TagExpression]
         */
        fun all(tags: Iterable<String>): TagExpression =
            AllTagsExpression(tags)

        /**
         * Returns a tag expression that is lazily evaluated from a [Provider]. The provider is only queried when
         * the tag expression is evaluated.
         *
         * @param provider a [Provider] providing the [TagExpression]
         * @return a [TagExpression] that lazily queries the provider when evaluated
         */
        fun fromProvider(provider: Provider<TagExpression>): TagExpression =
            ProviderTagExpression(provider)

        /**
         * Parses the input string as a tag expression.
         *
         * @param input the input string
         * @return the parsed [TagExpression]
         */
        fun parse(input: String): TagExpression =
            TagExpressionParser.parse(input)
    }
}


internal object AlwaysMatchTagExpression : TagExpression {

    override fun matches(tags: Set<String>): Boolean = true

    override fun and(other: TagExpression): TagExpression = other

    override fun or(other: TagExpression): TagExpression = this
}


internal class SingleTagExpression(val tag: String) : TagExpression {

    override fun matches(tags: Set<String>) =
        tag in tags


    override fun and(other: TagExpression): TagExpression =
        when (other) {
            is SingleTagExpression -> AllTagsExpression(listOf(this.tag, other.tag))
            is AllTagsExpression -> AllTagsExpression(other.tags + this.tag)
            else -> super.and(other)
        }


    override fun or(other: TagExpression): TagExpression =
        when (other) {
            is SingleTagExpression -> AnyTagExpression(listOf(this.tag, other.tag))
            is AnyTagExpression -> AnyTagExpression(other.tags + this.tag)
            else -> super.or(other)
        }
}


internal class AnyTagExpression(val tags: Iterable<String>) : TagExpression {

    override fun matches(tags: Set<String>): Boolean =
        this.tags.any { it in tags }


    override fun or(other: TagExpression): TagExpression =
        when (other) {
            is SingleTagExpression -> AnyTagExpression(this.tags + other.tag)
            is AnyTagExpression -> AnyTagExpression(this.tags + other.tags)
            else -> super.or(other)
        }
}


internal class AllTagsExpression(val tags: Iterable<String>) : TagExpression {

    override fun matches(tags: Set<String>): Boolean =
        this.tags.all { it in tags }


    override fun and(other: TagExpression): TagExpression =
        when (other) {
            is SingleTagExpression -> AllTagsExpression(this.tags + other.tag)
            is AllTagsExpression -> AllTagsExpression(this.tags + other.tags)
            else -> super.and(other)
        }
}


internal class OrTagExpression(val expressions: List<TagExpression>): TagExpression {

    override fun matches(tags: Set<String>): Boolean =
        expressions.any { it.matches(tags) }


    override fun or(other: TagExpression): TagExpression =
        when (other) {
            is AlwaysMatchTagExpression -> other
            is OrTagExpression -> OrTagExpression(this.expressions + other.expressions)
            else -> OrTagExpression(expressions + other)
        }
}


internal class AndTagExpression(val expressions: List<TagExpression>): TagExpression {

    override fun matches(tags: Set<String>): Boolean =
        expressions.all { it.matches(tags) }


    override fun and(other: TagExpression): TagExpression =
        when (other) {
            is AlwaysMatchTagExpression -> this
            is AndTagExpression -> AndTagExpression(this.expressions + other.expressions)
            else -> AndTagExpression(expressions + other)
        }
}


internal class NotTagExpression(val expression: TagExpression): TagExpression {

    override fun matches(tags: Set<String>): Boolean =
        !expression.matches(tags)


    override fun not(): TagExpression =
        when (expression) {
            is NotTagExpression -> expression.expression
            else -> super.not()
        }
}


internal class ProviderTagExpression(val provider: Provider<TagExpression>): TagExpression {

    override fun matches(tags: Set<String>): Boolean =
        provider.get().matches(tags)
}
