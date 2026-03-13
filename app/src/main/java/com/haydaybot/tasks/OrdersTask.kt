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

    // ── Truck orders ──────────────────────────────────────────────────────────

    private suspend fun fillTruckOrders() {
        val bmp = screen() ?: return
        val board = vision.find(bmp, "orders", "order_board") ?: run {
            log("Order board not visible")
            return
        }
        tapMatch(board)
        delay(800)

        var filled  = 0
        var skipped = 0

        repeat(10) {
            val s = screen() ?: return
            val slot = vision.find(s, "orders", "order_slot") ?: return

            tapMatch(slot)
            delay(500)

            val orderScreen = screen() ?: return

            // Can't fill?
            if (vision.find(orderScreen, "orders", "order_not_enough") != null) {
                log("Cannot fill — not enough items")
                vision.find(orderScreen, "orders", "order_skip_btn")?.let {
                    tapMatch(it)
                    skipped++
                } ?: input.swipe(640, 1200, 640, 400, durationMs = 200)
                delay(300)
                return@repeat
            }

            // Fill
            vision.find(orderScreen, "orders", "order_fill_btn")?.let {
                tapMatch(it)
                filled++
                BotStats.addOrder()
                log("Order filled ✓")
                delay(600)
            } ?: input.swipe(640, 1200, 640, 400, durationMs = 200)
        }

        log("Truck: filled=$filled skipped=$skipped")
        // Close board
        input.swipe(640, 1200, 640, 400, durationMs = 200)
        delay(400)
    }

    // ── Roadside shop ─────────────────────────────────────────────────────────

    private suspend fun restockShop() {
        val bmp = screen() ?: return
        val empties = vision.findAll(bmp, "orders", "shop_slot_empty")
        if (empties.isEmpty()) {
            log("No empty shop slots")
            return
        }

        var restocked = 0
        for (slot in empties.take(5)) {
            tapMatch(slot)
            delay(500)

            val addScreen = screen() ?: continue
            val addBtn = vision.find(addScreen, "orders", "shop_add_btn") ?: run {
                input.swipe(640, 1200, 640, 400, durationMs = 200)
                continue
            }
            tapMatch(addBtn)
            delay(400)

            val pickScreen = screen() ?: continue
            val item = vision.find(pickScreen, "orders", "shop_item_select") ?: run {
                input.swipe(640, 1200, 640, 400, durationMs = 200)
                input.swipe(640, 1200, 640, 400, durationMs = 200)
                continue
            }
            tapMatch(item)
            delay(400)

            val confScreen = screen() ?: continue
            vision.find(confScreen, "orders", "shop_confirm")?.let {
                tapMatch(it)
                restocked++
                log("Shop slot restocked ✓")
            } ?: input.swipe(640, 1200, 640, 400, durationMs = 200)

            delay(300)
        }
        log("Restocked $restocked shop slot(s)")
    }
}
