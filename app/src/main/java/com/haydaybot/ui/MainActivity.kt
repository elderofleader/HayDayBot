package com.haydaybot.ui

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.method.ScrollingMovementMethod
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import com.haydaybot.R
import com.haydaybot.databinding.ActivityMainBinding
import com.haydaybot.service.BotAccessibilityService
import com.haydaybot.service.BotForegroundService
import com.haydaybot.service.ScreenshotService
import com.haydaybot.utils.BotConfig
import com.haydaybot.utils.BotLogger
import com.haydaybot.utils.BotStats
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private var screenshotResultCode: Int = -1
    private var screenshotData: Intent? = null
    private lateinit var switchCrops: SwitchMaterial
    private lateinit var switchAnimals: SwitchMaterial
    private lateinit var switchFactory: SwitchMaterial
    private lateinit var switchOrders: SwitchMaterial

    private val screenshotPermLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            screenshotResultCode = result.resultCode
            screenshotData = result.data
            b.btnScreenshot.backgroundTintList = getColorStateList(android.R.color.holo_green_dark)
            b.btnScreenshot.text = "✓ Screenshot Permission Granted"
            addLog("Screenshot permission granted ✓")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        switchCrops   = b.toggleCrops.findViewById(R.id.switchTask)
        switchAnimals = b.toggleAnimals.findViewById(R.id.switchTask)
        switchFactory = b.toggleFactory.findViewById(R.id.switchTask)
        switchOrders  = b.toggleOrders.findViewById(R.id.switchTask)

        b.toggleCrops.findViewById<android.widget.TextView>(R.id.tvTaskName).text   = "🌽  Crops"
        b.toggleAnimals.findViewById<android.widget.TextView>(R.id.tvTaskName).text = "🐄  Animals"
        b.toggleFactory.findViewById<android.widget.TextView>(R.id.tvTaskName).text = "🏭  Factory"
        b.toggleOrders.findViewById<android.widget.TextView>(R.id.tvTaskName).text  = "📦  Orders"

        setupButtons()
        observeStats()
        observeLogs()
        b.tvLog.movementMethod = ScrollingMovementMethod()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionButtons()
        refreshStartStopButton()
    }

    private fun setupButtons() {
        b.btnAccessibility.setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        b.btnOverlay.setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        b.btnScreenshot.setOnClickListener {
            val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            screenshotPermLauncher.launch(mpm.createScreenCaptureIntent())
        }
        b.btnStartStop.setOnClickListener {
            if (BotAccessibilityService.isRunning) stopBot() else startBot()
        }
    }

    private fun startBot() {
        val svc = BotAccessibilityService.instance ?: run { addLog("⚠ Enable Accessibility first!"); return }
        if (screenshotData == null) { addLog("⚠ Grant Screenshot permission first!"); return }
        startForegroundService(Intent(this, ScreenshotService::class.java).apply {
            putExtra(ScreenshotService.EXTRA_CODE, screenshotResultCode)
            putExtra(ScreenshotService.EXTRA_DATA, screenshotData)
        })
        startForegroundService(Intent(this, BotForegroundService::class.java))
        svc.startBot(BotConfig(
            cropsEnabled = switchCrops.isChecked, animalsEnabled = switchAnimals.isChecked,
            factoryEnabled = switchFactory.isChecked, ordersEnabled = switchOrders.isChecked
        ))
        refreshStartStopButton(); addLog("Bot started ▶")
    }

    private fun stopBot() {
        BotAccessibilityService.instance?.stopBot()
        stopService(Intent(this, BotForegroundService::class.java))
        refreshStartStopButton(); addLog("Bot stopped ■")
    }

    private fun observeStats() {
        lifecycleScope.launch { BotStats.harvests.collectLatest { b.tvHarvests.text = it.toString() } }
        lifecycleScope.launch { BotStats.orders.collectLatest { b.tvOrders.text = it.toString() } }
        lifecycleScope.launch { BotStats.produced.collectLatest { b.tvProduced.text = it.toString() } }
        lifecycleScope.launch { BotStats.taskLabel.collectLatest {
            b.tvCurrentTask.text = it
            b.tvStatus.text = if (BotAccessibilityService.isRunning) "Running — $it" else getString(R.string.status_idle)
        }}
    }

    private fun observeLogs() {
        lifecycleScope.launch { BotLogger.logFlow.collectLatest { addLog(it) } }
    }

    private fun addLog(line: String) {
        runOnUiThread {
            val lines = b.tvLog.text.toString().split("\n").takeLast(60)
            b.tvLog.text = (lines + line).joinToString("\n")
            val layout = b.tvLog.layout ?: return@runOnUiThread
            val scrollY = layout.getLineTop(b.tvLog.lineCount) - b.tvLog.height
            if (scrollY > 0) b.tvLog.scrollTo(0, scrollY)
        }
    }

    private fun refreshPermissionButtons() {
        val a = BotAccessibilityService.instance != null
        b.btnAccessibility.backgroundTintList = getColorStateList(if (a) android.R.color.holo_green_dark else android.R.color.holo_orange_dark)
        b.btnAccessibility.text = if (a) "✓ Accessibility Enabled" else getString(R.string.perm_accessibility)
        val o = Settings.canDrawOverlays(this)
        b.btnOverlay.backgroundTintList = getColorStateList(if (o) android.R.color.holo_green_dark else android.R.color.holo_orange_dark)
        b.btnOverlay.text = if (o) "✓ Overlay Enabled" else getString(R.string.perm_overlay)
    }

    private fun refreshStartStopButton() {
        val r = BotAccessibilityService.isRunning
        b.btnStartStop.text = if (r) getString(R.string.btn_stop) else getString(R.string.btn_start)
        b.btnStartStop.backgroundTintList = getColorStateList(if (r) android.R.color.holo_red_dark else android.R.color.holo_green_dark)
        b.tvStatus.text = if (r) getString(R.string.status_running) else getString(R.string.status_idle)
    }
}
