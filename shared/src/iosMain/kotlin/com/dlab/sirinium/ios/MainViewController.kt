package com.dlab.sirinium.ios

import androidx.compose.ui.window.ComposeUIViewController
import com.dlab.sirinium.di.initKoin
import com.dlab.sirinium.ui.SiriniumAppContent
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    SiriniumAppContent()
}

fun initKoinIos() {
    initKoin()
}
