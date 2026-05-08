package com.neurodumpling.app

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import com.neurodumpling.app.ui.MindMapCanvas
import com.neurodumpling.app.ui.value.DarkBg
import com.neurodumpling.app.ui.value.LightBg
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

fun main() = application {
    val viewModel = remember { com.neurodumpling.app.viewmodel.MindMapViewModel() }
    val isDark by viewModel.isDarkMode.collectAsState()

    Window(
        onCloseRequest = ::exitApplication,
        title = "NeuroDumpling Desktop (v1.1)",
        icon = painterResource("drawables/logo.png"),
        state = rememberWindowState(width = 1280.dp, height = 800.dp),
    ) {
        MaterialTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = if (isDark) DarkBg else LightBg
            ) {
                MindMapCanvas(viewModel)
            }
        }
    }
}
