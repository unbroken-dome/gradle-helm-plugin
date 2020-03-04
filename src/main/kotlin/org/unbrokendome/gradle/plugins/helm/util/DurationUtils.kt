package org.unbrokendome.gradle.plugins.helm.util

import java.time.Duration


private val HelmDurationRegex =
    Regex("""^\\s*(?:(?<hours>[0-9]+)h)?(?:(?<minutes>[0-9]+)m)?(?:(?<seconds>[0-9]+)s)?\\s*$""")


internal fun tryParseHelmDuration(input: String): Duration? =
    HelmDurationRegex.matchEntire(input)?.let { match ->
        val hours = match.groups["hours"]?.value?.toInt() ?: 0
        val minutes = match.groups["minutes"]?.value?.toInt() ?: 0
        val seconds = match.groups["seconds"]?.value?.toInt() ?: 0
        Duration.parse("PT${hours}H${minutes}M${seconds}S")
    }


internal fun Duration.toHelmString() =
    this.toString()
        .substring(2) // remove the "PT" prefix
        .toLowerCase()
