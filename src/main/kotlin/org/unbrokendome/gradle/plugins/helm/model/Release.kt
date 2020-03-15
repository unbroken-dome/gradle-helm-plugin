package org.unbrokendome.gradle.plugins.helm.model

import org.json.JSONObject


/**
 * Contains information about a release (output of `helm ls`).
 */
internal data class Release(
    val name: String,
    val namespace: String,
    val revision: Int,
    val updated: String,
    val status: ReleaseStatus,
    val chart: String,
    val appVersion: String
) {

    companion object {

        fun fromJson(json: JSONObject) =
            Release(
                name = json.getString("name"),
                namespace = json.getString("namespace"),
                revision = json.get("revision")?.let { if (it is Number) it.toInt() else it.toString().toInt() } ?: 0,
                updated = json.getString("updated"),
                status = ReleaseStatus.parse(json.getString("status")),
                chart = json.getString("chart"),
                appVersion = json.getString("app_version")
            )
    }
}
