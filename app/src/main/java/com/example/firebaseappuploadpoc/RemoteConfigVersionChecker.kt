package com.example.firebaseappuploadpoc

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

object RemoteConfigVersionChecker {

    // Default JSON used when Remote Config is unreachable (first launch / no internet)
    private val defaultJson = """
        {
          "latest_version_code": 0,
          "force_update": false,
          "download_url": "",
          "release_notes": "A new version is available."
        }
    """.trimIndent()

    suspend fun checkVersion(): VersionInfo? {
        return try {
            val remoteConfig = Firebase.remoteConfig

            // 0 = always fetch fresh (good for testing)
            // Change to 3600 for production (max once per hour)
            remoteConfig.setConfigSettingsAsync(
                remoteConfigSettings { minimumFetchIntervalInSeconds = 0 }
            ).await()

            // Set default JSON so app works even if fetch fails
            remoteConfig.setDefaultsAsync(
                mapOf("update_config" to defaultJson)
            ).await()

            // Fetch latest config from Firebase and activate it
            remoteConfig.fetchAndActivate().await()

            // Read the single JSON parameter
            val jsonString = remoteConfig.getString("update_config")
            val json = JSONObject(jsonString)

            VersionInfo(
                latestVersionCode = json.optInt("latest_version_code", 0),
                forceUpdate       = json.optBoolean("force_update", false),
                downloadUrl       = json.optString("download_url", ""),
                releaseNotes      = json.optString("release_notes", "A new version is available.")
            )
        } catch (e: Exception) {
            // On any error: don't block the user, proceed normally
            null
        }
    }
}
