package com.haydaybot.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.haydaybot.tasks.*
import com.haydaybot.utils.BotConfig
import com.haydaybot.utils.BotLogger
import com.haydaybot.utils.BotStats
import kotlinx.coroutines.*

/**
 * Core bot service — runs on the Accessibility thread.
 * Spawns all task coroutines and manages the scheduler loop.
 */
class BotAccessibilityService : AccessibilityService() {

    companion object {
        var instance: BotAccessibilityService? = null
            private set

        var isRunning: Boolean = false
            private set
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var input: InputController
    private lateinit var vision: VisionEngine
    private var config = BotConfig()

    // ── Service lifecycle ─────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        input    = InputController(this)
        vision   = VisionEngine(this)
        BotLogger.let { /* Initialised as singleton */ }
    }

    override fun onDestroy() {
        stopBot()
        scope.cancel()
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* not used */ }
    override fun onInterrupt() { stopBot() }

    // ── Bot control ───────────────────────────────────────────────────────────

    fun startBot(cfg: BotConfig = BotConfig()) {
        if (isRunning) return
        config    = cfg
        isRunning = true
        BotStats.reset()
        scope.launch { runScheduler() }
    }

    fun stopBot() {
        isRunning = false
        scope.coroutineContext.cancelChildren()
    }

    // ── Scheduler ─────────────────────────────────────────────────────────────

    private suspend fun runScheduler() {
        BotLogger.i("Scheduler", "Bot started")
        BotStats.setTask("Starting…")

        val tasks = buildList {
            if (config.cropsEnabled)   add(CropTask(input, vision, config))
            if (config.animalsEnabled) add(AnimalTask(input, vision, config))
            if (config.factoryEnabled) add(FactoryTask(input, vision, config))
            if (config.ordersEnabled)  add(OrdersTask(input, vision, config))
        }

        var lastIdleBreak = System.currentTimeMillis()

        while (isRunning) {
            val now = System.currentTimeMillis()

            // Anti-ban idle break
            if (now - lastIdleBreak >= config.idleFrequencyMs) {
                doIdleBreak()
                lastIdleBreak = System.currentTimeMillis()
            }

            var anyRan = false
            for (task in tasks) {
                if (!isRunning) break
                if (task.isDue()) {
                    BotStats.setTask(task.displayName)
                    BotLogger.i("Scheduler", "▶ ${task.displayName}")
                    try {
                        task.run()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        BotLogger.e("Scheduler", "Task ${task.displayName} failed", e)
                    }
                    anyRan = true
                }
            }

            if (!anyRan) {
                BotStats.setTask("Idle")
                delay(5_000)
            }
        }

        BotStats.setTask("Stopped")
        BotLogger.i("Scheduler", "Bot stopped")
    }

    private suspend fun doIdleBreak() {
        val duration = (config.idleBreakMinMs..config.idleBreakMaxMs).random()
        BotLogger.i("Scheduler", "Anti-ban idle break: ${duration / 1000}s")
        BotStats.setTask("Idle break")
        val end = System.currentTimeMillis() + duration
        while (System.currentTimeMillis() < end && isRunning) {
            // Random scroll to look human
            val actions = listOf(
                { scope.launch { input.swipe(400, 300, 600, 400) } },
                { scope.launch { input.swipe(600, 400, 400, 300) } },
                { null }
            )
            actions.random().invoke()
            delay((3_000L..10_000L).random())
        }
    }

    // ── Screenshot helper ─────────────────────────────────────────────────────

    fun getScreen() = ScreenshotService.getFrame()
}
