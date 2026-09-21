package com.dlab.sirinium.core.util

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object AppLogger {
    private const val MAX_LOGS = 120
    private val logBuffer = mutableListOf<String>()
    private var lastCrashLog: String? = null

    fun d(tag: String, message: String) = log("DEBUG", tag, message)
    fun i(tag: String, message: String) = log("INFO", tag, message)
    fun w(tag: String, message: String) = log("WARN", tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMsg = if (throwable != null) {
            "$message\n${throwable.stackTraceToString()}"
        } else {
            message
        }
        log("ERROR", tag, fullMsg)
    }

    fun recordCrash(crash: String) {
        lastCrashLog = crash
        log("CRASH", "System", crash)
    }

    fun getLastCrashLog(): String? = lastCrashLog

    fun setLastCrashLog(crash: String?) {
        lastCrashLog = crash
    }

    fun getRecentLogs(): String {
        return try {
            logBuffer.toList().joinToString("\n")
        } catch (_: Throwable) {
            ""
        }
    }

    private fun log(level: String, tag: String, message: String) {
        val now = try {
            val dt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val h = dt.hour.toString().padStart(2, '0')
            val m = dt.minute.toString().padStart(2, '0')
            val s = dt.second.toString().padStart(2, '0')
            "$h:$m:$s"
        } catch (_: Throwable) {
            ""
        }
        val entry = "[$now] [$level] [$tag] $message"
        println(entry)
        try {
            if (logBuffer.size >= MAX_LOGS) {
                logBuffer.removeAt(0)
            }
            logBuffer.add(entry)
        } catch (_: Throwable) {}
    }
}
