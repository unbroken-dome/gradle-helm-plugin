package com.citi.gradle.plugins.helm.util

private const val BytesPerKB = 1024L
private const val BytesPerMB = 1024L * 1024L
private const val BytesPerGB = 1024L * 1024L * 1024L


/**
 * Formats the given amount of bytes according to its order of magnitude.
 *
 * * Numbers < 1KB will be formatted as "x B"
 * * Numbers < 1MB will be formatted as "x KB" (with one fractional digit)
 * * Numbers < 1GB will be formatted as "x MB" (with one fractional digit)
 * * Higher numbers will be formatted as "x GB" (with one fractional digit)
 *
 * @param bytes the amount of bytes to be formatted
 * @return the formatted string
 */
internal fun formatDataSize(bytes: Long): String {
    return when {
        bytes < BytesPerKB -> {
            "$bytes B"
        }
        bytes < BytesPerMB -> {
            val kb = (bytes * 10 / BytesPerKB).toFloat() * 0.1f
            String.format("%.1f KB", kb)
        }
        bytes < BytesPerGB -> {
            val mb = (bytes * 10 / BytesPerMB).toFloat() * 0.1f
            String.format("%.1f MB", mb)
        }
        else -> {
            val gb = (bytes * 10 / BytesPerGB).toFloat() * 0.1f
            String.format("%.1f GB", gb)
        }
    }
}
