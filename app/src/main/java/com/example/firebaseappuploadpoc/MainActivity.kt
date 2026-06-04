package com.example.firebaseappuploadpoc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.firebaseappuploadpoc.ui.theme.FirebaseAppUploadPOCTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FirebaseAppUploadPOCTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "splash") {
                    composable("splash") {
                        SplashScreen(onNavigateToLogin = {
                            navController.navigate("login") {
                                popUpTo("splash") { inclusive = true }
                            }
                        })
                    }
                    composable("login") {
                        LoginScreen()
                    }
                }
            }
        }
    }
}
