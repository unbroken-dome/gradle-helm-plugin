package com.citi.gradle.plugins.helm.util

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
    val bestUnit = when {
        bytes < UnitOfMeasurement.Kilobytes.bytesInUnit -> UnitOfMeasurement.Bytes
        bytes < UnitOfMeasurement.MegaBytes.bytesInUnit -> UnitOfMeasurement.Kilobytes
        bytes < UnitOfMeasurement.Gigabytes.bytesInUnit -> UnitOfMeasurement.MegaBytes
        else -> UnitOfMeasurement.Gigabytes
    }

    return bestUnit.formatValue(bytes)
}

private const val UNIT_DIVIDER = 1024L

private enum class UnitOfMeasurement(private val unitName: String, val bytesInUnit: Long) {
    Bytes("B", 1),
    Kilobytes("KB", UNIT_DIVIDER),
    MegaBytes("MB", UNIT_DIVIDER * UNIT_DIVIDER),
    Gigabytes("GB", UNIT_DIVIDER * UNIT_DIVIDER * UNIT_DIVIDER);

    private companion object {
        private const val numberFormat = "%.1f"
    }

    private val bytesInUnitFloat = bytesInUnit.toFloat()

    fun formatValue(value: Long): String {
        val divided = value / bytesInUnitFloat

        return "${String.format(numberFormat, divided)} $unitName"
    }
}
