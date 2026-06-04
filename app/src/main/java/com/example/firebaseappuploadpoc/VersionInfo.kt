package com.example.firebaseappuploadpoc

data class VersionInfo(
    val latestVersionCode: Int = 0,
    val forceUpdate: Boolean = false,
    val downloadUrl: String = "",
    val releaseNotes: String = "A new version is available."
)

sealed class UpdateState {
    object Loading : UpdateState()
    object UpToDate : UpdateState()
    data class UpdateRequired(val info: VersionInfo) : UpdateState()
}
