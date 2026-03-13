package com.haydaybot.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Live stats shown in the UI. */
object BotStats {
    private val _harvests  = MutableStateFlow(0)
    private val _orders    = MutableStateFlow(0)
    private val _produced  = MutableStateFlow(0)
    private val _taskLabel = MutableStateFlow("Idle")

    val harvests   = _harvests.asStateFlow()
    val orders     = _orders.asStateFlow()
    val produced   = _produced.asStateFlow()
    val taskLabel  = _taskLabel.asStateFlow()

    fun addHarvest(n: Int = 1)  { _harvests.value  += n }
    fun addOrder(n: Int = 1)    { _orders.value    += n }
    fun addProduced(n: Int = 1) { _produced.value  += n }
    fun setTask(label: String)  { _taskLabel.value  = label }

    fun reset() {
        _harvests.value  = 0
        _orders.value    = 0
        _produced.value  = 0
        _taskLabel.value = "Idle"
    }
}
