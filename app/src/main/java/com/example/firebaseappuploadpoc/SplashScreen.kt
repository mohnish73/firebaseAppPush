package com.example.firebaseappuploadpoc

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firebaseappuploadpoc.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onNavigateToLogin: () -> Unit) {
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Loading) }

    LaunchedEffect(Unit) {
        // Run animation and version check in parallel
        val animJob = launch {
            scale.animateTo(1f, animationSpec = tween(700))
            alpha.animateTo(1f, animationSpec = tween(500))
            delay(1000)
        }

        val info = FirestoreVersionChecker.checkVersion()
        updateState = if (
            info != null &&
            info.latestVersionCode > BuildConfig.VERSION_CODE &&
            info.forceUpdate
        ) {
            UpdateState.UpdateRequired(info)
        } else {
            UpdateState.UpToDate
        }

        animJob.join() // wait for splash animation before acting

        if (updateState is UpdateState.UpToDate) {
            onNavigateToLogin()
        }
        // If UpdateRequired, ForceUpdateDialog below takes over
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A237E), Color(0xFF3949AB))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(scale.value)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "FB",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "FirebaseApp",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Powered by Firebase",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .size(28.dp)
                    .alpha(alpha.value)
            )
        }
    }

    if (updateState is UpdateState.UpdateRequired) {
        val info = (updateState as UpdateState.UpdateRequired).info
        ForceUpdateDialog(
            downloadUrl = info.downloadUrl,
            releaseNotes = info.releaseNotes
        )
    }
}
