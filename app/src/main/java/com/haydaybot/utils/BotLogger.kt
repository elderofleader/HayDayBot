package com.haydaybot.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * In-app logger — emits log lines as a Flow so the UI can display them.
 */
object BotLogger {
    private const val TAG = "HayDayBot"
    private val _logFlow = MutableSharedFlow<String>(replay = 200)
    val logFlow = _logFlow.asSharedFlow()

    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    suspend fun i(tag: String, msg: String) {
        Log.i(TAG, "[$tag] $msg")
        emit("I", tag, msg)
    }

    suspend fun d(tag: String, msg: String) {
        Log.d(TAG, "[$tag] $msg")
        emit("D", tag, msg)
    }

    suspend fun w(tag: String, msg: String) {
        Log.w(TAG, "[$tag] $msg")
        emit("W", tag, msg)
    }

    suspend fun e(tag: String, msg: String, t: Throwable? = null) {
        Log.e(TAG, "[$tag] $msg", t)
        emit("E", tag, "$msg${t?.let { " — ${it.message}" } ?: ""}")
    }

    private suspend fun emit(level: String, tag: String, msg: String) {
        val time = fmt.format(Date())
        _logFlow.emit("$time [$level/$tag] $msg")
    }
}
