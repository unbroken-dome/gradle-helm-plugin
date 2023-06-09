package com.citi.gradle.plugins.helm.model

import org.json.JSONObject


/**
 * Contains information about a release (output of `helm ls`).
 */
internal interface Release {

    val name: String
    val namespace: String
    val revision: Int
    val updated: String
    val status: ReleaseStatus
    val chart: String
    val appVersion: String


    companion object {

        fun fromJson(json: JSONObject): Release =
            JsonRelease(json)
    }
}


private class JsonRelease(
    private val json: JSONObject
) : Release {

    override val name: String
        get() = json.getString("name")

    override val namespace: String
        get() = json.getString("namespace")

    override val revision: Int
        get() = json.get("revision")?.let { if (it is Number) it.toInt() else it.toString().toInt() } ?: 0

    override val updated: String
        get() = json.getString("updated")

    override val status: ReleaseStatus
        get() = ReleaseStatus.parse(json.getString("status"))

    override val chart: String
        get() = json.getString("chart")

    override val appVersion: String
        get() = json.getString("app_version")
}




