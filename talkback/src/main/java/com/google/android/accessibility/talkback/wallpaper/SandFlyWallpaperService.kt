/*
 * Copyright 2026 SandFly contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.accessibility.talkback.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.File

/**
 * SandFly's live wallpaper. Android keeps the current wallpaper's service bound and its process
 * running, and restarts it if it stops, as it would a visible app. As this runs in SandFly's own
 * process, phone makers' battery savers that stop background apps, such as Vivo's, leave SandFly
 * running too.
 *
 * It shows the image the user chose in SandFly, or black, and draws only when Android asks.
 */
class SandFlyWallpaperService : WallpaperService() {

  override fun onCreateEngine(): Engine = ImageEngine()

  private inner class ImageEngine : Engine() {
    private var image: Bitmap? = null
    private var imageStamp = 0L
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)

    override fun onCreate(surfaceHolder: SurfaceHolder) {
      super.onCreate(surfaceHolder)
      engines += this
    }

    override fun onDestroy() {
      engines -= this
      image?.recycle()
      image = null
      super.onDestroy()
    }

    override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
      super.onSurfaceChanged(holder, format, width, height)
      draw()
    }

    override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) {
      super.onSurfaceRedrawNeeded(holder)
      draw()
    }

    override fun onVisibilityChanged(visible: Boolean) {
      super.onVisibilityChanged(visible)
      if (visible) draw()
    }

    fun draw() {
      val holder = surfaceHolder ?: return
      val canvas =
        try {
          holder.lockCanvas()
        } catch (e: IllegalStateException) {
          null
        } ?: return
      try {
        canvas.drawColor(Color.BLACK)
        val bitmap = currentImage()
        if (bitmap != null) {
          // Fills the screen, cropping what doesn't fit, around the middle.
          val scale =
            maxOf(
              canvas.width / bitmap.width.toFloat(),
              canvas.height / bitmap.height.toFloat(),
            )
          val matrix = Matrix()
          matrix.setScale(scale, scale)
          matrix.postTranslate(
            (canvas.width - bitmap.width * scale) / 2,
            (canvas.height - bitmap.height * scale) / 2,
          )
          canvas.drawBitmap(bitmap, matrix, paint)
        }
      } finally {
        try {
          holder.unlockCanvasAndPost(canvas)
        } catch (e: IllegalStateException) {
          LogUtils.w(TAG, "Wallpaper surface went away while drawing")
        }
      }
    }

    /** The chosen image, read again when it changed. */
    private fun currentImage(): Bitmap? {
      val file = imageFile(this@SandFlyWallpaperService)
      val stamp = if (file.exists()) file.lastModified() else 0L
      if (stamp != imageStamp) {
        image?.recycle()
        image =
          if (stamp == 0L) {
            null
          } else {
            try {
              BitmapFactory.decodeFile(file.path)
            } catch (e: Throwable) {
              // A broken file or running out of memory leaves the wallpaper black.
              LogUtils.e(TAG, "Cannot read the wallpaper image: %s", e)
              null
            }
          }
        imageStamp = stamp
      }
      return image
    }
  }

  companion object {
    private const val TAG = "SandFlyWallpaper"
    private val engines = mutableSetOf<ImageEngine>()

    /** Where the image the user chose is kept, already scaled to the screen. */
    @JvmStatic fun imageFile(context: Context): File = File(context.filesDir, "wallpaper.png")

    /** Draws the running wallpapers again, after the image changed. Main thread only. */
    @JvmStatic
    fun redraw() {
      engines.toList().forEach { it.draw() }
    }

    /** Whether SandFly is the wallpaper now. */
    @JvmStatic
    fun isCurrent(context: Context): Boolean =
      try {
        WallpaperManager.getInstance(context).wallpaperInfo?.component ==
          ComponentName(context, SandFlyWallpaperService::class.java)
      } catch (e: RuntimeException) {
        false
      }

    /** Opens Android's screen for setting SandFly as the wallpaper. */
    @JvmStatic
    fun intentToSet(context: Context): Intent =
      Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        .putExtra(
          WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
          ComponentName(context, SandFlyWallpaperService::class.java),
        )
  }
}
