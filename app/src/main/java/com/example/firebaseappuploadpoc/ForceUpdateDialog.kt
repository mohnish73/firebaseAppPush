package com.example.firebaseappuploadpoc

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.firebaseappuploadpoc.ui.theme.White
import kotlinx.coroutines.delay
import java.io.File

private enum class DownloadState { IDLE, DOWNLOADING, ERROR }

@Composable
fun ForceUpdateDialog(downloadUrl: String, releaseNotes: String) {
    val context = LocalContext.current
    val primaryBlue = Color(0xFF1A237E)
    val accentBlue  = Color(0xFF3949AB)

    var downloadState      by remember { mutableStateOf(DownloadState.IDLE) }
    var progress           by remember { mutableFloatStateOf(0f) }
    var downloadId         by remember { mutableLongStateOf(-1L) }

    val apkFile = remember {
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "app-update.apk")
    }

    // True if APK was already downloaded in a previous attempt
    var isAlreadyDownloaded by remember { mutableStateOf(apkFile.exists() && apkFile.length() > 0) }

    val downloadManager = remember {
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }

    // Triggers the Android native installer
    fun installApk() {
        try {
            val apkUri = if (downloadId != -1L) {
                // Fresh download just completed — use DownloadManager URI
                downloadManager.getUriForDownloadedFile(downloadId)
            } else {
                // APK was already on disk from previous download — use FileProvider
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            isAlreadyDownloaded = true
        } catch (e: Exception) {
            downloadState = DownloadState.ERROR
        }
    }

    // Poll download progress every 500ms
    LaunchedEffect(downloadId) {
        if (downloadId == -1L) return@LaunchedEffect

        while (downloadState == DownloadState.DOWNLOADING) {
            val query  = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)

            if (cursor.moveToFirst()) {
                val downloaded = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                )
                val total = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                )
                val status = cursor.getInt(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                )

                if (total > 0) progress = downloaded.toFloat() / total.toFloat()

                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        downloadState = DownloadState.IDLE
                        installApk() // launch native installer immediately
                    }
                    DownloadManager.STATUS_FAILED -> {
                        downloadState = DownloadState.ERROR
                    }
                }
            }
            cursor.close()
            if (downloadState != DownloadState.DOWNLOADING) break
            delay(500)
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress    = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(accentBlue.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = null,
                        tint = accentBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Update Required",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryBlue
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "A new version of the app is available. You must update to continue.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                if (releaseNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "What's new",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = releaseNotes,
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            lineHeight = 19.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                when (downloadState) {

                    DownloadState.IDLE -> {
                        Button(
                            onClick = {
                                if (isAlreadyDownloaded) {
                                    // APK already on device — skip download, go straight to install
                                    installApk()
                                } else {
                                    // Fresh download
                                    downloadState = DownloadState.DOWNLOADING
                                    progress = 0f

                                    val request = DownloadManager.Request(Uri.parse(downloadUrl))
                                        .setTitle("App Update")
                                        .setDescription("Downloading new version...")
                                        .setDestinationInExternalFilesDir(
                                            context,
                                            Environment.DIRECTORY_DOWNLOADS,
                                            "app-update.apk"
                                        )
                                        .setNotificationVisibility(
                                            DownloadManager.Request.VISIBILITY_VISIBLE
                                        )
                                        .setMimeType("application/vnd.android.package-archive")

                                    downloadId = downloadManager.enqueue(request)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isAlreadyDownloaded)
                                        Icons.Filled.SystemUpdate else Icons.Filled.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    // Shows "Install Now" if already downloaded, "Update Now" if not
                                    text = if (isAlreadyDownloaded) "Install Now" else "Update Now",
                                    fontSize = 15.sp,
                                    color = White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    DownloadState.DOWNLOADING -> {
                        Text(
                            text = "Downloading update...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = accentBlue
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = accentBlue,
                            trackColor = accentBlue.copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }

                    DownloadState.ERROR -> {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Download failed. Please try again.",
                            fontSize = 13.sp,
                            color = Color.Red,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                // On retry delete the partial file and start fresh
                                apkFile.takeIf { it.exists() }?.delete()
                                isAlreadyDownloaded = false
                                downloadState = DownloadState.IDLE
                                downloadId = -1L
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(text = "Retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This update is mandatory",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}
