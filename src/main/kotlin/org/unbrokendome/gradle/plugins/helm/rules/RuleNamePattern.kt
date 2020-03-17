package org.unbrokendome.gradle.plugins.helm.rules

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectCollection
import org.unbrokendome.gradle.plugins.helm.util.capitalizeWords


/**
 * A naming pattern for target names that are camel-cased and contain a variable part.
 */
internal class RuleNamePattern(
    /** The part before the variable part. May be an empty string. */
    private val prefix: String,
    /** The placeholder for the variable part. */
    private val placeholder: String,
    /** The part after the variable part. May be an empty string. */
    private val suffix: String
) {

    /**
     * Maps a source name to a target name.
     *
     * The source name will be converted to camel case and inserted as the variable part of the name pattern.
     *
     * @param sourceName the source name
     * @return the target name
     */
    fun mapName(sourceName: String): String =
        prefix + sourceName.capitalizeWords() + suffix


    /**
     * Checks if the given target name is a possible match for this pattern.
     *
     * @param targetName the target name
     * @return `true` if the target name matches
     */
    fun matches(targetName: String): Boolean =
        targetName.length > prefix.length + suffix.length &&
                targetName.startsWith(prefix) &&
                targetName.endsWith(suffix) &&
                targetName[prefix.length].isUpperCase()


    /**
     * Finds a source object that would map to the given target name according to this pattern.
     *
     * @param targetName the target name
     * @param sourceContainer a [NamedDomainObjectCollection] containing all known source objects
     * @param <S> the type of source object
     * @return the matching source object, or `null` if no match was found
     */
    fun <S : Named> findSource(targetName: String, sourceContainer: NamedDomainObjectCollection<S>): S? {
        val variablePart = targetName.substring(prefix.length, targetName.length - suffix.length)
        // first, assume the original source name was starting with lower case
        return sourceContainer.findByName(variablePart.decapitalize())
            // otherwise also check for upper case
            ?: sourceContainer.findByName(variablePart)
            // otherwise do a "reverse" find by checking all existing source objects with the mapName function
            ?: sourceContainer.find { mapName(it.name) == targetName }
    }


    override fun toString(): String =
        "Pattern: $prefix<$placeholder>$suffix"


    companion object {

        private val regex = Regex("^(?<prefix>[^<]*)<(?<placeholder>[^>]+)>(?<suffix>.*)$")

        /**
         * Constructs a [RuleNamePattern] from the given input string.
         *
         * It must contain a single placeholder enclosed in angle brackets, e.g. `helmSome<Chart>Task`
         *
         * @param input the input string
         * @return the parsed [RuleNamePattern]
         */
        fun parse(input: String): RuleNamePattern =
            regex.matchEntire(input)?.let { result ->
                val (prefix, placeholder, suffix) = result.destructured
                RuleNamePattern(prefix, placeholder, suffix)
            } ?: throw IllegalArgumentException("Invalid RuleNamePattern: $input")
    }
}


/**
 * A naming pattern for target names that are camel-cased and contain a single placeholder.
 */
internal class RuleNamePattern2(
    /** The part before the first variable part. May be an empty string. */
    private val prefix: String,
    /** The placeholder for the first variable part. */
    private val firstPlaceholder: String,
    /** The part between the two variable parts. May be an empty string. */
    private val middlePart: String,
    /** The placeholder for the second variable part. */
    private val secondPlaceholder: String,
    /** The part after the second variable part. May be an empty string. */
    private val suffix: String
) {

    /** A regular expression that represents this pattern. */
    private val regex = Regex(
        '^' + Regex.escape(prefix) +
                "(\\p{Upper}\\p{Alnum}*)" +
                Regex.escape(middlePart) +
                "(\\p{Upper}\\p{Alnum}*)" +
                Regex.escape(suffix) + '$'
    )


    /**
     * Maps the given source names to a target name.
     *
     * The source names will be converted to camel case and inserted as the variable parts of the name pattern.
     *
     * @param sourceName1 the first source name
     * @param sourceName2 the second source name
     * @return the target name
     */
    fun mapName(sourceName1: String, sourceName2: String): String =
        prefix + sourceName1.capitalizeWords() + middlePart + sourceName2.capitalizeWords() + suffix


    /**
     * Checks if the given target name is a possible match for this pattern.
     *
     * @param targetName the target name
     * @return `true` if the target name matches
     */
    fun matches(targetName: String) =
        regex.matches(targetName)


    /**
     * Finds a pair of source objects that would map to the given target name according to this pattern.
     *
     * @param targetName the target name
     * @param sourceContainer1 a [NamedDomainObjectCollection] containing all known source objects of the first type
     * @param sourceContainer2 a [NamedDomainObjectCollection] containing all known source objects of the second type
     * @param <S1> the first type of source object
     * @param <S2> the second type of source object
     * @return a [Pair] containing the matching source objects, or `null` if no match was found
     */
    fun <S1 : Named, S2 : Named> findSources(
        targetName: String,
        sourceContainer1: NamedDomainObjectCollection<S1>,
        sourceContainer2: NamedDomainObjectCollection<S2>
    ): Pair<S1, S2>? =
        regex.matchEntire(targetName)?.let { result ->
            val (varPart1, varPart2) = result.destructured

            val source1Candidate = sourceContainer1.findByName(varPart1.decapitalize())
                ?: sourceContainer1.findByName(varPart1)
            val source2Candidate = sourceContainer2.findByName(varPart2.decapitalize())
                ?: sourceContainer2.findByName(varPart2)

            if (source1Candidate != null) {

                if (source2Candidate != null) {
                    // found both items directly, we can return the pair
                    source1Candidate to source2Candidate

                } else {
                    // We only found a match for the first item.
                    // Try to "reverse"-find the second item by looking through all
                    val source1Name = source1Candidate.name
                    sourceContainer2.find { mapName(source1Name, it.name) == targetName }
                        ?.let { source1Candidate to it }
                }

            } else {
                if (source2Candidate != null) {
                    // We only found a match for the first item.
                    // Try to "reverse"-find the second item by looking through all
                    val source2Name = source2Candidate.name
                    sourceContainer1.find { mapName(it.name, source2Name) == targetName }
                        ?.let { it to source2Candidate }

                } else {
                    // We found neither the first nor the second item.
                    // Try to "reverse"-find on both containers
                    sourceContainer1.cartesianProductWith(sourceContainer2)
                        .firstOrNull { (source1, source2) ->
                            mapName(source1.name, source2.name) == targetName
                        }
                }
            }
        }


    override fun toString(): String =
        "$prefix<$firstPlaceholder>$middlePart<$secondPlaceholder>$suffix"


    private fun <T, U> Iterable<T>.cartesianProductWith(other: Iterable<U>) =
        sequence {
            for (item1 in this@cartesianProductWith) {
                for (item2 in other) {
                    yield(item1 to item2)
                }
            }
        }


    companion object {

        private val regex = Regex(
            "^(?<prefix>[^<]*)<(?<placeholder1>[^>]+)>(?<middle>[^<]*)<(?<placeholder2>[^>]+)>(?<suffix>.*)$"
        )


        /**
         * Constructs a [RuleNamePattern2] from the given input string.
         *
         * It must contain two placeholders enclosed in angle brackets, e.g.
         * `helmSome<Chart>And<Repo>Task`
         *
         * @param input the input string
         * @return the parsed [RuleNamePattern2]
         */
        fun parse(input: String): RuleNamePattern2 =
            regex.matchEntire(input)?.let { result ->
                val (prefix, placeholder1, middlePart, placeholder2, suffix) = result.destructured
                RuleNamePattern2(prefix, placeholder1, middlePart, placeholder2, suffix)
            } ?: throw IllegalArgumentException("Invalid RuleNamePattern: $input")
    }
}
