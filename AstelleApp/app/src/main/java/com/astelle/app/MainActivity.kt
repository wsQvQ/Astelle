package com.astelle.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.astelle.app.ui.navigation.AstelleNavHost
import com.astelle.app.ui.theme.AstelleTheme
import com.astelle.app.ui.theme.LocalAstelleColors
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AstelleTheme {
                val colors = LocalAstelleColors.current
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colors.paper,
                ) {
                    AstelleNavHost()
                }
            }
        }
    }
}
