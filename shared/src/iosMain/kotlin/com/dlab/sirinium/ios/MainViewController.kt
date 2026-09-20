package com.dlab.sirinium.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeUIViewController
import com.dlab.sirinium.di.initKoin
import com.dlab.sirinium.ui.SiriniumAppContent
import org.koin.compose.KoinContext
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIViewController
import kotlin.experimental.ExperimentalNativeApi

private var koinInitialized = false
private const val KEY_CRASH_LOG = "last_sirinium_crash_log"

@OptIn(ExperimentalNativeApi::class)
fun startKoin() {
    if (!koinInitialized) {
        kotlin.native.setUnhandledExceptionHook { throwable ->
            val message = throwable.message ?: "Unknown error"
            val stack = throwable.stackTraceToString()
            println("[Sirinium iOS Crash] Unhandled exception: $message\n$stack")
            try {
                NSUserDefaults.standardUserDefaults.setObject(
                    "[Crash] $message\n$stack",
                    forKey = KEY_CRASH_LOG
                )
                NSUserDefaults.standardUserDefaults.synchronize()
            } catch (_: Throwable) {}
        }
        try {
            initKoin()
            koinInitialized = true
        } catch (e: Throwable) {
            println("[Sirinium iOS initKoin Error]: ${e.message}")
            try {
                NSUserDefaults.standardUserDefaults.setObject(
                    "[initKoin Error] ${e.message}\n${e.stackTraceToString()}",
                    forKey = KEY_CRASH_LOG
                )
                NSUserDefaults.standardUserDefaults.synchronize()
            } catch (_: Throwable) {}
        }
    }
}

@Composable
private fun IosCrashFallback(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sirinium",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Зафиксирован предыдущий сбой приложения",
                fontSize = 15.sp,
                color = Color(0xFFF87171),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = errorMessage,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Text("Продолжить", color = Color.White)
            }
        }
    }
}

fun MainViewController(): UIViewController {
    startKoin()
    return ComposeUIViewController {
        var crashLog by remember {
            mutableStateOf(
                NSUserDefaults.standardUserDefaults.stringForKey(KEY_CRASH_LOG)
            )
        }

        if (crashLog != null) {
            IosCrashFallback(
                errorMessage = crashLog!!,
                onDismiss = {
                    NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY_CRASH_LOG)
                    NSUserDefaults.standardUserDefaults.synchronize()
                    crashLog = null
                }
            )
        } else {
            KoinContext {
                SiriniumAppContent()
            }
        }
    }
}
