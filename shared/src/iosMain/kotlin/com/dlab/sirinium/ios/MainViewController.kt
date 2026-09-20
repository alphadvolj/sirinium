package com.dlab.sirinium.ios

import androidx.compose.ui.window.ComposeUIViewController
import com.dlab.sirinium.di.initKoin
import com.dlab.sirinium.ui.SiriniumAppContent
import platform.UIKit.UIViewController

import kotlin.experimental.ExperimentalNativeApi

private var koinInitialized = false

@OptIn(ExperimentalNativeApi::class)
fun startKoin() {
    if (!koinInitialized) {
        kotlin.native.setUnhandledExceptionHook { throwable ->
            println("[Sirinium iOS Crash] Unhandled exception: ${throwable.message}")
            throwable.printStackTrace()
        }
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
