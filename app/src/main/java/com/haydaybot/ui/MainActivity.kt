package com.haydaybot.ui

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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

    // ── Permission launchers ──────────────────────────────────────────────────

    private val screenshotPermLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            screenshotResultCode = result.resultCode
            screenshotData       = result.data
            b.btnScreenshot.backgroundTintList =
                getColorStateList(android.R.color.holo_green_dark)
            b.btnScreenshot.text = "✓ Screenshot Permission Granted"
            addLog("Screenshot permission granted ✓")
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupTaskToggles()
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

    // ── UI setup ──────────────────────────────────────────────────────────────

    private fun setupTaskToggles() {
        b.toggleCrops.findViewById<TextView>(
            com.haydaybot.R.id.tvTaskName).text   = "🌽  Crops"
        b.toggleAnimals.findViewById<TextView>(
            com.haydaybot.R.id.tvTaskName).text   = "🐄  Animals"
        b.toggleFactory.findViewById<TextView>(
            com.haydaybot.R.id.tvTaskName).text   = "🏭  Factory"
        b.toggleOrders.findViewById<TextView>(
            com.haydaybot.R.id.tvTaskName).text   = "📦  Orders"
    }

    private fun setupButtons() {
        // Accessibility permission
        b.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        // Overlay permission
        b.btnOverlay.setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }

        // Screenshot permission
        b.btnScreenshot.setOnClickListener {
            val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            screenshotPermLauncher.launch(mpm.createScreenCaptureIntent())
        }

        // Start / Stop
        b.btnStartStop.setOnClickListener {
            if (BotAccessibilityService.isRunning) {
                stopBot()
            } else {
                startBot()
            }
        }
    }

    // ── Start / Stop ──────────────────────────────────────────────────────────

    private fun startBot() {
        val svc = BotAccessibilityService.instance
        if (svc == null) {
            addLog("⚠ Enable Accessibility permission first!")
            return
        }
        if (screenshotData == null) {
            addLog("⚠ Grant Screenshot permission first!")
            return
        }

        // Start screenshot capture service
        startForegroundService(
            Intent(this, ScreenshotService::class.java).apply {
                putExtra(ScreenshotService.EXTRA_CODE, screenshotResultCode)
                putExtra(ScreenshotService.EXTRA_DATA, screenshotData)
            }
        )

        // Start foreground service (keeps process alive)
        startForegroundService(Intent(this, BotForegroundService::class.java))

        // Build config from toggles
        val cfg = BotConfig(
            cropsEnabled   = b.toggleCrops.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(com.haydaybot.R.id.switchTask).isChecked,
            animalsEnabled = b.toggleAnimals.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(com.haydaybot.R.id.switchTask).isChecked,
            factoryEnabled = b.toggleFactory.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(com.haydaybot.R.id.switchTask).isChecked,
            ordersEnabled  = b.toggleOrders.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(com.haydaybot.R.id.switchTask).isChecked,
        )

        svc.startBot(cfg)
        refreshStartStopButton()
        addLog("Bot started ▶")
    }

    private fun stopBot() {
        BotAccessibilityService.instance?.stopBot()
        stopService(Intent(this, BotForegroundService::class.java))
        refreshStartStopButton()
        addLog("Bot stopped ■")
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun observeStats() {
        lifecycleScope.launch {
            BotStats.harvests.collectLatest  { b.tvHarvests.text = it.toString() }
        }
        lifecycleScope.launch {
            BotStats.orders.collectLatest    { b.tvOrders.text   = it.toString() }
        }
        lifecycleScope.launch {
            BotStats.produced.collectLatest  { b.tvProduced.text = it.toString() }
        }
        lifecycleScope.launch {
            BotStats.taskLabel.collectLatest {
                b.tvCurrentTask.text = it
                b.tvStatus.text = if (BotAccessibilityService.isRunning)
                    "Running — $it" else getString(com.haydaybot.R.string.status_idle)
            }
        }
    }

    private fun observeLogs() {
        lifecycleScope.launch {
            BotLogger.logFlow.collectLatest { line ->
                addLog(line)
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun addLog(line: String) {
        runOnUiThread {
            val current = b.tvLog.text.toString()
            val lines   = current.split("\n").takeLast(60)
            b.tvLog.text = (lines + line).joinToString("\n")
            // Scroll to bottom
            val layout = b.tvLog.layout
            if (layout != null) {
                val scrollY = layout.getLineTop(b.tvLog.lineCount) - b.tvLog.height
                if (scrollY > 0) b.tvLog.scrollTo(0, scrollY)
            }
        }
    }

    private fun refreshPermissionButtons() {
        // Accessibility
        val accessEnabled = BotAccessibilityService.instance != null
        b.btnAccessibility.backgroundTintList = if (accessEnabled)
            getColorStateList(android.R.color.holo_green_dark)
        else
            getColorStateList(com.google.android.material.R.color.design_default_color_secondary)
        b.btnAccessibility.text = if (accessEnabled)
            "✓ Accessibility Enabled" else getString(com.haydaybot.R.string.perm_accessibility)

        // Overlay
        val overlayEnabled = Settings.canDrawOverlays(this)
        b.btnOverlay.backgroundTintList = if (overlayEnabled)
            getColorStateList(android.R.color.holo_green_dark)
        else
            getColorStateList(com.google.android.material.R.color.design_default_color_secondary)
        b.btnOverlay.text = if (overlayEnabled)
            "✓ Overlay Enabled" else getString(com.haydaybot.R.string.perm_overlay)
    }

    private fun refreshStartStopButton() {
        val running = BotAccessibilityService.isRunning
        b.btnStartStop.text = if (running)
            getString(com.haydaybot.R.string.btn_stop)
        else
            getString(com.haydaybot.R.string.btn_start)
        b.btnStartStop.backgroundTintList = if (running)
            getColorStateList(android.R.color.holo_red_dark)
        else
            getColorStateList(android.R.color.holo_green_dark)
        b.tvStatus.text = if (running)
            getString(com.haydaybot.R.string.status_running)
        else
            getString(com.haydaybot.R.string.status_idle)
    }
}
