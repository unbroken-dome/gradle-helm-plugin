package com.citi.gradle.plugins.helm.model


internal enum class ReleaseStatus(private val value: String) {
    DEPLOYED("deployed"),
    UNINSTALLED("uninstalled"),
    UNINSTALLING("uninstalling"),
    PENDING("pending"),
    PENDING_UPGRADE("pending_upgrade"),
    PENDING_ROLLBACK("pending_rollback"),
    SUPERSEDED("superseded"),
    FAILED("failed"),
    UNKNOWN("unknown");

    companion object {

        fun parse(value: String) =
            values().find { it.value == value }
                ?: UNKNOWN
    }
}
