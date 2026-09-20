package com.dlab.sirinium.ios

import androidx.compose.ui.window.ComposeUIViewController
import com.dlab.sirinium.di.initKoin
import com.dlab.sirinium.ui.SiriniumAppContent
import platform.UIKit.UIViewController

private var koinInitialized = false

fun startKoin() {
    if (!koinInitialized) {
        initKoin()
        koinInitialized = true
    }
}

fun MainViewController(): UIViewController {
    startKoin()
    return ComposeUIViewController {
        SiriniumAppContent()
    }
}
