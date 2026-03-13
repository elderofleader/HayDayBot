package com.haydaybot.tasks

import com.haydaybot.service.InputController
import com.haydaybot.service.VisionEngine
import com.haydaybot.utils.BotConfig
import com.haydaybot.utils.BotStats
import kotlinx.coroutines.delay

class CropTask(
    input: InputController,
    vision: VisionEngine,
    config: BotConfig,
) : BaseTask(input, vision, config) {

    override val displayName = "Crops"
    override val cycleMs     = config.cropCycleMs

    override suspend fun run() {
        log("── Crop cycle start ──")
        try {
            harvestAll()
            plantAll()
        } catch (e: Exception) {
            err("Crop task error", e)
        } finally {
            markDone()
            log("── Crop cycle done ──")
        }
    }

    private suspend fun harvestAll() {
        var harvested = 0
        repeat(30) {
            val bmp = screen() ?: return
            if (vision.find(bmp, "crops", "inventory_full") != null) {
                warn("Barn full — skipping harvest")
                return
            }
            val match = vision.find(bmp, "crops", "harvest_ready") ?: return
            tapMatch(match)
            delay(500)
            // Confirm harvest button if present
            screen()?.let { s ->
                vision.find(s, "crops", "harvest_btn")?.let {
                    tapMatch(it)
                    harvested++
                }
            }
        }
        BotStats.addHarvest(harvested)
        log("Harvested $harvested plot(s)")
    }

    private suspend fun plantAll() {
        var planted = 0
        repeat(30) {
            val bmp = screen() ?: return
            val empty = vision.find(bmp, "crops", "plot_empty") ?: return
            tapMatch(empty)
            delay(400)

            for (crop in config.cropList) {
                val seedScreen = screen() ?: return
                val seed = vision.find(seedScreen, "crops", "seed_$crop")
                if (seed != null) {
                    tapMatch(seed)
                    planted++
                    delay(300)
                    return@repeat
                }
            }
            // No seeds — close dialog
            input.swipe(0, 800, 0, 800, durationMs = 50) // tap outside
        }
        log("Planted $planted plot(s)")
    }
}
