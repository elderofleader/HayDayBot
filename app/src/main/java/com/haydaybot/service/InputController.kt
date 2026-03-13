package com.haydaybot.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PointF
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.random.Random

/**
 * Sends tap / swipe / long-press gestures through the AccessibilityService.
 * All coordinates are in raw screen pixels.
 */
class InputController(private val service: AccessibilityService) {

    // ── Tap ───────────────────────────────────────────────────────────────────

    suspend fun tap(x: Int, y: Int, jitter: Boolean = true) {
        val jx = if (jitter) x + Random.nextInt(-6, 7) else x
        val jy = if (jitter) y + Random.nextInt(-6, 7) else y
        dispatchGesture(buildTap(jx.toFloat(), jy.toFloat(), durationMs = 80))
        randomDelay()
    }

    suspend fun tap(pt: PointF, jitter: Boolean = true) = tap(pt.x.toInt(), pt.y.toInt(), jitter)

    // ── Long press ────────────────────────────────────────────────────────────

    suspend fun longPress(x: Int, y: Int, durationMs: Long = 800) {
        dispatchGesture(buildTap(x.toFloat(), y.toFloat(), durationMs))
        randomDelay()
    }

    // ── Swipe ─────────────────────────────────────────────────────────────────

    suspend fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long = 300) {
        dispatchGesture(buildSwipe(x1, y1, x2, y2, durationMs))
        randomDelay()
    }

    // ── Builder helpers ───────────────────────────────────────────────────────

    private fun buildTap(x: Float, y: Float, durationMs: Long): GestureDescription {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        return GestureDescription.Builder().addStroke(stroke).build()
    }

    private fun buildSwipe(
        x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long
    ): GestureDescription {
        val path = Path().apply {
            moveTo(x1.toFloat(), y1.toFloat())
            lineTo(x2.toFloat(), y2.toFloat())
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        return GestureDescription.Builder().addStroke(stroke).build()
    }

    // ── Dispatch ──────────────────────────────────────────────────────────────

    private suspend fun dispatchGesture(gesture: GestureDescription): Boolean =
        suspendCancellableCoroutine { cont ->
            val cb = object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    cont.resume(true)
                }
                override fun onCancelled(gestureDescription: GestureDescription) {
                    cont.resume(false)
                }
            }
            if (!service.dispatchGesture(gesture, cb, null)) {
                cont.resume(false)
            }
        }

    // ── Anti-ban delay ────────────────────────────────────────────────────────

    private suspend fun randomDelay() {
        val ms = Random.nextLong(400, 1200)
        kotlinx.coroutines.delay(ms)
    }
}
