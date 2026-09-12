package com.aiassistant.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import java.nio.ByteBuffer

object ScreenCapture {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var lastBitmap: Bitmap? = null

    fun requestPermission(activity: Activity) {
        val manager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager
        activity.startActivityForResult(
            manager.createScreenCaptureIntent(),
            9001
        )
    }

    fun start(context: Context, resultCode: Int, data: Intent) {
        try {
            val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                    as MediaProjectionManager
            mediaProjection = manager.getMediaProjection(resultCode, data)

            val metrics = DisplayMetrics()
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = wm.currentWindowMetrics.bounds
                metrics.widthPixels = bounds.width()
                metrics.heightPixels = bounds.height()
            } else {
                @Suppress("DEPRECATION")
                wm.defaultDisplay.getRealMetrics(metrics)
            }

            val density = metrics.densityDpi
            val width = metrics.widthPixels
            val height = metrics.heightPixels

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "AIAssistantScreenCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                Handler(Looper.getMainLooper())
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun capture(): Bitmap? {
        val reader = imageReader ?: return null
        var image: Image? = null
        try {
            image = reader.acquireLatestImage() ?: return lastBitmap
            val planes = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            val cropped = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            lastBitmap = cropped
            return cropped
        } catch (e: Exception) {
            e.printStackTrace()
            return lastBitmap
        } finally {
            image?.close()
        }
    }

    fun stop() {
        try {
            virtualDisplay?.release()
            mediaProjection?.stop()
            imageReader?.close()
        } catch (_: Exception) {}
        virtualDisplay = null
        mediaProjection = null
        imageReader = null
        lastBitmap = null
    }

    fun isReady(): Boolean = imageReader != null
}
