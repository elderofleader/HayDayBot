package com.haydaybot.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScreenshotService : Service() {

    companion object {
        const val CHANNEL_ID   = "screenshot_channel"
        const val NOTIF_ID     = 2
        const val EXTRA_CODE   = "result_code"
        const val EXTRA_DATA   = "result_data"

        private val _lastBitmap = MutableStateFlow<Bitmap?>(null)
        val lastBitmap = _lastBitmap.asStateFlow()

        /** Called by BotAccessibilityService to get the latest frame. */
        fun getFrame(): Bitmap? = _lastBitmap.value
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var screenWidth  = 0
    private var screenHeight = 0
    private var screenDensity = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())

        val resultCode = intent?.getIntExtra(EXTRA_CODE, -1) ?: return START_NOT_STICKY
        val resultData = intent.getParcelableExtra<Intent>(EXTRA_DATA) ?: return START_NOT_STICKY

        val wm    = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm    = DisplayMetrics().also { wm.defaultDisplay.getMetrics(it) }
        screenWidth   = dm.widthPixels
        screenHeight  = dm.heightPixels
        screenDensity = dm.densityDpi

        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mpm.getMediaProjection(resultCode, resultData)

        imageReader = ImageReader.newInstance(
            screenWidth, screenHeight, PixelFormat.RGBA_8888, 2
        )

        virtualDisplay = projection?.createVirtualDisplay(
            "HayDayBotCapture",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride  = planes[0].pixelStride
            val rowStride    = planes[0].rowStride
            val rowPadding   = rowStride - pixelStride * screenWidth
            val bmp = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight, Bitmap.Config.ARGB_8888
            )
            bmp.copyPixelsFromBuffer(buffer)
            _lastBitmap.value = Bitmap.createBitmap(bmp, 0, 0, screenWidth, screenHeight)
            bmp.recycle()
            image.close()
        }, null)

        return START_STICKY
    }

    override fun onDestroy() {
        virtualDisplay?.release()
        projection?.stop()
        imageReader?.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Screenshot Service", NotificationManager.IMPORTANCE_LOW
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hay Day Bot")
            .setContentText("Capturing screen…")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
}
