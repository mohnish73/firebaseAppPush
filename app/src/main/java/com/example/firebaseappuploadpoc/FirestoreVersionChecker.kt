package com.example.firebaseappuploadpoc

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object FirestoreVersionChecker {

    suspend fun checkVersion(): VersionInfo? {
        return try {
            val doc = Firebase.firestore
                .collection("app_config")
                .document("version_info")
                .get()
                .await()

            if (!doc.exists()) return null

            VersionInfo(
                latestVersionCode = doc.getLong("latestVersionCode")?.toInt() ?: 0,
                forceUpdate = doc.getBoolean("forceUpdate") ?: false,
                downloadUrl = doc.getString("downloadUrl") ?: "",
                releaseNotes = doc.getString("releaseNotes") ?: "A new version is available."
            )
        } catch (e: Exception) {
            // Network error or Firestore unavailable — don't block the user
            null
        }
    }
}
