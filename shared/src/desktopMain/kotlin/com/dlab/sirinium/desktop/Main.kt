package com.dlab.sirinium.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.dlab.sirinium.di.initKoin
import com.dlab.sirinium.ui.SiriniumAppContent

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Sirinium",
            state = rememberWindowState(width = 450.dp, height = 820.dp)
        ) {
            SiriniumAppContent()
        }
    }
}
