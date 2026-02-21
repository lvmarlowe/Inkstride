package com.inkstride.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.inkstride.app.ui.AppRouter
import com.inkstride.app.ui.theme.InkstrideTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InkstrideTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppRouter(innerPadding = innerPadding)
                }
            }
        }
    }
}