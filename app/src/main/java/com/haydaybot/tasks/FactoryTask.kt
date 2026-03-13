package com.haydaybot.tasks

import com.haydaybot.service.InputController
import com.haydaybot.service.VisionEngine
import com.haydaybot.utils.BotConfig
import com.haydaybot.utils.BotStats
import kotlinx.coroutines.delay

class FactoryTask(
    input: InputController,
    vision: VisionEngine,
    config: BotConfig,
) : BaseTask(input, vision, config) {

    override val displayName = "Factory"
    override val cycleMs     = config.factoryCycleMs

    companion object {
        /** machine_name → (done_template, make_template, product_template) */
        val MACHINE_MAP = mapOf(
            "bakery"      to Triple("bakery_done",   "bakery_make",   "bread"),
            "sugar_mill"  to Triple("sugar_done",    "sugar_make",    "sugar"),
            "feed_mill"   to Triple("feed_done",     "feed_make",     "chicken_feed"),
            "popcorn_pot" to Triple("popcorn_done",  "popcorn_make",  "popcorn"),
            "loom"        to Triple("loom_done",     "loom_make",     "wool_cloth"),
            "dairy"       to Triple("dairy_done",    "dairy_make",    "cream"),
            "pie_oven"    to Triple("pie_done",      "pie_make",      "apple_pie"),
        )
    }

    override suspend fun run() {
        log("── Factory cycle start ──")
        try {
            for (machine in config.machines) {
                val entry = MACHINE_MAP[machine]
                if (entry == null) {
                    warn("Unknown machine '$machine' — skipping")
                    continue
                }
                processMachine(machine, entry.first, entry.second, entry.third)
            }
        } catch (e: Exception) {
            err("Factory task error", e)
        } finally {
            markDone()
            log("── Factory cycle done ──")
        }
    }

    private suspend fun processMachine(
        machine: String,
        doneTpl: String,
        makeTpl: String,
        productTpl: String,
    ) {
        val bmp = screen() ?: return

        // 1. Collect finished goods
        val done = vision.find(bmp, "factory", doneTpl)
        if (done != null) {
            log("$machine done — collecting")
            tapMatch(done)
            delay(500)
            screen()?.let { s ->
                vision.find(s, "factory", "collect_btn")?.let {
                    tapMatch(it)
                    BotStats.addProduced()
                    delay(400)
                }
            }
        }

        // 2. Re-queue production
        if (!config.autoRequeue) return

        val bmp2 = screen() ?: return
        val make = vision.find(bmp2, "factory", makeTpl) ?: return

        log("$machine — queueing $productTpl")
        tapMatch(make)
        delay(500)

        val prodScreen = screen() ?: return

        // Check for missing ingredients
        if (vision.find(prodScreen, "factory", "no_ingredients") != null) {
            warn("$machine: missing ingredients for $productTpl")
            input.swipe(640, 1200, 640, 400, durationMs = 300) // swipe down to dismiss
            return
        }

        val prod = vision.find(prodScreen, "factory", productTpl)
        if (prod != null) {
            tapMatch(prod)
            delay(300)
            screen()?.let { s ->
                vision.find(s, "factory", "produce_btn")?.let {
                    tapMatch(it)
                    log("$machine: started $productTpl")
                }
            }
        } else {
            // Dismiss machine dialog
            input.swipe(640, 1200, 640, 400, durationMs = 300)
        }
    }
}
