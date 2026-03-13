package com.haydaybot.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc

/**
 * OpenCV template matching — Android port of the Python image_recognition module.
 *
 * Templates are stored as assets:  assets/templates/<category>/<name>.png
 */
class VisionEngine(private val context: Context) {

    data class MatchResult(
        val name: String,
        val confidence: Float,
        val rect: Rect,
    ) {
        val center: PointF get() = PointF(
            (rect.left + rect.width() / 2f),
            (rect.top  + rect.height() / 2f)
        )
    }

    private val templateCache = HashMap<String, Mat>()
    private val confidenceThreshold = 0.80f

    // ── Public API ────────────────────────────────────────────────────────────

    fun find(screen: Bitmap, category: String, name: String): MatchResult? {
        val screenMat = bitmapToMat(screen)
        val tplMat = loadTemplate(category, name) ?: return null
        return matchSingle(screenMat, tplMat, name, confidenceThreshold)
    }

    fun findAll(screen: Bitmap, category: String, name: String): List<MatchResult> {
        val screenMat = bitmapToMat(screen)
        val tplMat = loadTemplate(category, name) ?: return emptyList()
        return matchAll(screenMat, tplMat, name, confidenceThreshold)
    }

    fun findAny(screen: Bitmap, category: String, names: List<String>): MatchResult? {
        for (name in names) {
            val result = find(screen, category, name)
            if (result != null) return result
        }
        return null
    }

    fun pixelColor(screen: Bitmap, x: Int, y: Int): Triple<Int, Int, Int> {
        val px = screen.getPixel(x, y)
        return Triple(
            android.graphics.Color.red(px),
            android.graphics.Color.green(px),
            android.graphics.Color.blue(px)
        )
    }

    // ── Template loading ──────────────────────────────────────────────────────

    private fun loadTemplate(category: String, name: String): Mat? {
        val key = "$category/$name"
        templateCache[key]?.let { return it }

        return try {
            val stream = context.assets.open("templates/$category/$name.png")
            val bytes  = stream.readBytes()
            stream.close()
            val buf = MatOfByte(*bytes)
            val mat = Imgcodecs.imdecode(buf, Imgcodecs.IMREAD_COLOR)
            if (mat.empty()) null
            else {
                templateCache[key] = mat
                mat
            }
        } catch (e: Exception) {
            null  // Template not found — task will skip this element
        }
    }

    // ── Matching ──────────────────────────────────────────────────────────────

    private fun matchSingle(
        screen: Mat, tpl: Mat, name: String, threshold: Float
    ): MatchResult? {
        val result = Mat()
        Imgproc.matchTemplate(screen, tpl, result, Imgproc.TM_CCOEFF_NORMED)
        val mmr = Core.minMaxLoc(result)
        if (mmr.maxVal >= threshold) {
            val loc = mmr.maxLoc
            val rect = Rect(
                loc.x.toInt(), loc.y.toInt(),
                tpl.cols(), tpl.rows()
            )
            return MatchResult(name, mmr.maxVal.toFloat(), rect)
        }
        return null
    }

    private fun matchAll(
        screen: Mat, tpl: Mat, name: String, threshold: Float
    ): List<MatchResult> {
        val result = Mat()
        Imgproc.matchTemplate(screen, tpl, result, Imgproc.TM_CCOEFF_NORMED)

        val matches = mutableListOf<MatchResult>()
        val data = FloatArray(1)
        for (row in 0 until result.rows()) {
            for (col in 0 until result.cols()) {
                result.get(row, col, data)
                if (data[0] >= threshold) {
                    val rect = Rect(col, row, tpl.cols(), tpl.rows())
                    matches.add(MatchResult(name, data[0], rect))
                }
            }
        }
        return nms(matches, 0.3f)
    }

    // ── Non-maximum suppression ───────────────────────────────────────────────

    private fun nms(matches: List<MatchResult>, iouThreshold: Float): List<MatchResult> {
        val sorted = matches.sortedByDescending { it.confidence }.toMutableList()
        val keep   = mutableListOf<MatchResult>()
        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            keep.add(best)
            sorted.removeAll { iou(best.rect, it.rect) >= iouThreshold }
        }
        return keep
    }

    private fun iou(a: Rect, b: Rect): Float {
        val inter = Rect(
            maxOf(a.left, b.left), maxOf(a.top, b.top),
            minOf(a.right, b.right), minOf(a.bottom, b.bottom)
        )
        if (inter.width() <= 0 || inter.height() <= 0) return 0f
        val ia = inter.width().toFloat() * inter.height()
        val ua = a.width() * a.height() + b.width() * b.height() - ia
        return ia / ua
    }

    // ── Util ──────────────────────────────────────────────────────────────────

    private fun bitmapToMat(bmp: Bitmap): Mat {
        val mat = Mat()
        Utils.bitmapToMat(bmp, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)
        return mat
    }
}
