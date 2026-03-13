package com.haydaybot.tasks

import com.haydaybot.service.InputController
import com.haydaybot.service.VisionEngine
import com.haydaybot.utils.BotConfig
import kotlinx.coroutines.delay

class AnimalTask(
    input: InputController,
    vision: VisionEngine,
    config: BotConfig,
) : BaseTask(input, vision, config) {

    override val displayName = "Animals"
    override val cycleMs     = config.animalCycleMs

    override suspend fun run() {
        log("── Animal cycle start ──")
        try {
            collectAll()
            feedAll()
        } catch (e: Exception) {
            err("Animal task error", e)
        } finally {
            markDone()
            log("── Animal cycle done ──")
        }
    }

    private suspend fun collectAll() {
        var collected = 0
        repeat(20) {
            val bmp = screen() ?: return
            val match = vision.find(bmp, "animals", "collect_ready") ?: return
            tapMatch(match)
            collected++
            delay(400)
            screen()?.let { s ->
                vision.find(s, "animals", "collect_btn")?.let { tapMatch(it) }
            }
        }
        log("Collected from $collected animal(s)")
    }

    private suspend fun feedAll() {
        var fed = 0
        repeat(20) {
            val bmp = screen() ?: return
            if (vision.find(bmp, "animals", "silo_empty") != null) {
                warn("Silo empty — cannot feed")
                return
            }
            val match = vision.find(bmp, "animals", "feed_bubble") ?: return
            tapMatch(match)
            delay(400)
            val btn = run {
                var b: VisionEngine.MatchResult? = null
                repeat(6) {
                    if (b == null) {
                        b = screen()?.let { s -> vision.find(s, "animals", "feed_btn") }
                        if (b == null) kotlinx.coroutines.runBlocking { delay(500) }
                    }
                }
                b
            }
            if (btn != null) {
                tapMatch(btn)
                fed++
            }
            delay(300)
        }
        log("Fed $fed animal(s)")
    }
}
