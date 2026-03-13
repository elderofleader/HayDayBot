package com.haydaybot.tasks

import com.haydaybot.service.InputController
import com.haydaybot.service.VisionEngine
import com.haydaybot.utils.BotConfig
import com.haydaybot.utils.BotStats
import kotlinx.coroutines.delay

class OrdersTask(
    input: InputController,
    vision: VisionEngine,
    config: BotConfig,
) : BaseTask(input, vision, config) {

    override val displayName = "Orders"
    override val cycleMs     = config.orderCycleMs

    override suspend fun run() {
        log("── Orders cycle start ──")
        try {
            if (config.truckOrders)  fillTruckOrders()
            if (config.roadsideShop) restockShop()
        } catch (e: Exception) {
            err("Orders task error", e)
        } finally {
            markDone()
            log("── Orders cycle done ──")
        }
    }

    private suspend fun fillTruckOrders() {
        val bmp = screen() ?: return
        val board = vision.find(bmp, "orders", "order_board") ?: return
        tapMatch(board)
        delay(800)

        var filled = 0; var skipped = 0; var attempts = 0
        while (attempts < 10) {
            attempts++
            val s = screen() ?: break
            val slot = vision.find(s, "orders", "order_slot") ?: break
            tapMatch(slot); delay(500)
            val os = screen() ?: break
            if (vision.find(os, "orders", "order_not_enough") != null) {
                val skip = vision.find(os, "orders", "order_skip_btn")
                if (skip != null) { tapMatch(skip); skipped++ }
                else input.swipe(640, 1200, 640, 400, durationMs = 200)
                delay(300); continue
            }
            val fillBtn = vision.find(os, "orders", "order_fill_btn")
            if (fillBtn != null) { tapMatch(fillBtn); filled++; BotStats.addOrder(); delay(600) }
            else input.swipe(640, 1200, 640, 400, durationMs = 200)
        }
        log("Truck: filled=$filled skipped=$skipped")
        input.swipe(640, 1200, 640, 400, durationMs = 200); delay(400)
    }

    private suspend fun restockShop() {
        val bmp = screen() ?: return
        val empties = vision.findAll(bmp, "orders", "shop_slot_empty")
        if (empties.isEmpty()) { log("No empty shop slots"); return }
        var restocked = 0; var i = 0
        while (i < minOf(empties.size, 5)) {
            tapMatch(empties[i++]); delay(500)
            val addScreen = screen() ?: continue
            val addBtn = vision.find(addScreen, "orders", "shop_add_btn")
                ?: run { input.swipe(640, 1200, 640, 400, durationMs = 200); continue }
            tapMatch(addBtn); delay(400)
            val pickScreen = screen() ?: continue
            val item = vision.find(pickScreen, "orders", "shop_item_select")
                ?: run { input.swipe(640, 1200, 640, 400, durationMs = 200); continue }
            tapMatch(item); delay(400)
            val confScreen = screen() ?: continue
            val conf = vision.find(confScreen, "orders", "shop_confirm")
            if (conf != null) { tapMatch(conf); restocked++; log("Shop slot restocked ✓") }
            else input.swipe(640, 1200, 640, 400, durationMs = 200)
            delay(300)
        }
        log("Restocked $restocked shop slot(s)")
    }
}
