package com.dlab.sirinium.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppUpdateDto(
    @SerialName("type") val type: String = "update",
    @SerialName("date") val date: String = "",
    @SerialName("time") val time: String = "",
    @SerialName("versionName") val versionName: String = "",
    @SerialName("versionCode") val versionCode: Int = 0,
    @SerialName("changelog") val changelog: String = "",
    @SerialName("RustoreLink") val rustoreLink: String = "",
    @SerialName("GithubRelease") val githubRelease: String = "",
    @SerialName("rustore_link") val rustoreLinkSnake: String = "",
    @SerialName("github_release") val githubReleaseSnake: String = ""
) {
    val effectiveRustoreLink: String
        get() = rustoreLink.ifBlank { rustoreLinkSnake }.ifBlank { "https://www.rustore.ru" }

    val effectiveGithubRelease: String
        get() = githubRelease.ifBlank { githubReleaseSnake }.ifBlank { "https://github.com/alphadvolj/sirinium/releases" }
}

@Serializable
data class AppInfoDto(
    @SerialName("type") val type: String = "info",
    @SerialName("date") val date: String = "",
    @SerialName("time") val time: String = "",
    @SerialName("text") val text: String = ""
)
