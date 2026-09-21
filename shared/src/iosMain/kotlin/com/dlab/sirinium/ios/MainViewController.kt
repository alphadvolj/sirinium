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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.dlab.sirinium.core.util.AppLogger
import com.dlab.sirinium.di.initKoin
import com.dlab.sirinium.ui.SiriniumAppContent
import org.koin.compose.KoinContext
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIPasteboard
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
            AppLogger.recordCrash("[Crash] $message\n$stack")
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
            AppLogger.recordCrash("[initKoin Error] ${e.message}\n${e.stackTraceToString()}")
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
    var isCopied by remember { mutableStateOf(false) }

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

            // 1. Send Report via Mail / Share
            Button(
                onClick = {
                    val subject = "[Sirinium iOS Crash Report]"
                    val body = buildString {
                        appendLine("=== SIRINIUM iOS CRASH REPORT ===")
                        appendLine("Версия приложения: 3.0.1")
                        try {
                            val device = UIDevice.currentDevice
                            appendLine("Устройство: ${device.model} (${device.systemName} ${device.systemVersion})")
                        } catch (_: Throwable) {}
                        appendLine()
                        appendLine("=== ТЕКСТ ОШИБКИ И СТЕК ===")
                        appendLine(errorMessage)
                        appendLine()
                        val recent = AppLogger.getRecentLogs()
                        if (recent.isNotBlank()) {
                            appendLine("=== ПОСЛЕДНИЕ СОБЫТИЯ И ЛОГИ ===")
                            appendLine(recent)
                        }
                    }
                    val encodedSubject = subject.replace(" ", "%20")
                    val encodedBody = body.replace("\n", "%0A").replace(" ", "%20")
                    val mailUrl = "mailto:dmitry@avh-vless.work?subject=$encodedSubject&body=$encodedBody"
                    val nsUrl = NSURL.URLWithString(mailUrl)
                    if (nsUrl != null && UIApplication.sharedApplication.canOpenURL(nsUrl)) {
                        UIApplication.sharedApplication.openURL(nsUrl)
                    } else {
                        val rootVc = UIApplication.sharedApplication.keyWindow?.rootViewController
                        if (rootVc != null) {
                            val activityVc = UIActivityViewController(
                                activityItems = listOf("$subject\n\n$body"),
                                applicationActivities = null
                            )
                            rootVc.presentViewController(activityVc, animated = true, completion = null)
                        } else {
                            UIPasteboard.generalPasteboard.string = "$subject\n\n$body"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Text("Отправить отчет на почту", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Copy Log
            OutlinedButton(
                onClick = {
                    UIPasteboard.generalPasteboard.string = errorMessage
                    isCopied = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isCopied) "Лог скопирован в буфер!" else "Скопировать лог",
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Continue
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
            ) {
                Text("Продолжить", color = Color.White)
            }
        }
    }
}

fun MainViewController(): UIViewController {
    startKoin()
    return ComposeUIViewController(
        configure = {
            enforceStrictPlistSanityCheck = false
        }
    ) {
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
            var feedbackTrigger by remember { mutableStateOf(0) }

            DisposableEffect(Unit) {
                val observer = NSNotificationCenter.defaultCenter.addObserverForName(
                    name = "deviceDidShakeNotification",
                    `object` = null,
                    queue = NSOperationQueue.mainQueue
                ) { _ ->
                    val isExplicitlyDisabled = NSUserDefaults.standardUserDefaults.objectForKey("pref_shake_to_report") != null &&
                            !NSUserDefaults.standardUserDefaults.boolForKey("pref_shake_to_report")
                    if (!isExplicitlyDisabled) {
                        feedbackTrigger++
                    }
                }
                onDispose {
                    NSNotificationCenter.defaultCenter.removeObserver(observer)
                }
            }

            KoinContext {
                SiriniumAppContent(
                    openFeedbackTrigger = feedbackTrigger
                )
            }
        }
    }
}
