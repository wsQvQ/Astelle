package com.astelle.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.astelle.app.ui.theme.LocalAstelleColors

@Composable
fun SettingsScreen(onOpenDrawer: () -> Unit) {
    val colors = LocalAstelleColors.current
    Scaffold(
        containerColor = colors.paper,
        topBar = {
            IconButton(onClick = onOpenDrawer, modifier = Modifier.padding(8.dp)) {
                Icon(
                    Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = "打开侧栏",
                    tint = colors.inkSoft,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("设置", style = MaterialTheme.typography.headlineMedium, color = colors.ink)
            Text(
                "本地优先，数据都在你手机里。",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
