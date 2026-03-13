package com.haydaybot.utils

/**
 * Central configuration — mirrors config.yaml from the Python bot.
 * In the APK, settings are stored in SharedPreferences.
 */
data class BotConfig(
    // Task toggles
    val cropsEnabled: Boolean = true,
    val animalsEnabled: Boolean = true,
    val factoryEnabled: Boolean = true,
    val ordersEnabled: Boolean = true,

    // Crop list priority (template names)
    val cropList: List<String> = listOf("wheat", "corn", "soybean", "sugarcane"),
    val plotCount: Int = 12,

    // Factory machines to monitor
    val machines: List<String> = listOf(
        "bakery", "sugar_mill", "feed_mill", "popcorn_pot", "loom"
    ),
    val autoRequeue: Boolean = true,

    // Orders
    val truckOrders: Boolean = true,
    val roadsideShop: Boolean = true,
    val sellThresholdPct: Int = 60,

    // Timing (ms)
    val actionDelayMinMs: Long = 400,
    val actionDelayMaxMs: Long = 1200,
    val cropCycleMs: Long = 300_000,      // 5 min
    val animalCycleMs: Long = 600_000,    // 10 min
    val factoryCycleMs: Long = 180_000,   // 3 min
    val orderCycleMs: Long = 240_000,     // 4 min
    val idleBreakMinMs: Long = 30_000,
    val idleBreakMaxMs: Long = 120_000,
    val idleFrequencyMs: Long = 1_800_000, // 30 min

    // Vision
    val confidenceThreshold: Float = 0.80f,
    val debugScreenshots: Boolean = false
)
