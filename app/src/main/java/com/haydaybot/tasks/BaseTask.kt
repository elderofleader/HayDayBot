package com.haydaybot.tasks

import android.graphics.Bitmap
import com.haydaybot.service.BotAccessibilityService
import com.haydaybot.service.InputController
import com.haydaybot.service.VisionEngine
import com.haydaybot.utils.BotConfig
import com.haydaybot.utils.BotLogger

abstract class BaseTask(
    protected val input: InputController,
    protected val vision: VisionEngine,
    protected val config: BotConfig,
) {
    abstract val displayName: String
    abstract val cycleMs: Long

    private var lastRun: Long = 0L

    fun isDue() = (System.currentTimeMillis() - lastRun) >= cycleMs
    fun markDone() { lastRun = System.currentTimeMillis() }

    abstract suspend fun run()

    protected suspend fun screen(): Bitmap? = BotAccessibilityService.instance?.getScreen()

    protected suspend fun tapMatch(match: VisionEngine.MatchResult) {
        input.tap(match.center)
    }

    protected suspend fun findAndTap(
        bmp: Bitmap, category: String, name: String
    ): Boolean {
        val match = vision.find(bmp, category, name) ?: return false
        tapMatch(match)
        return true
    }

    protected suspend fun log(msg: String) = BotLogger.i(displayName, msg)
    protected suspend fun warn(msg: String) = BotLogger.w(displayName, msg)
    protected suspend fun err(msg: String, t: Throwable? = null) = BotLogger.e(displayName, msg, t)
}
