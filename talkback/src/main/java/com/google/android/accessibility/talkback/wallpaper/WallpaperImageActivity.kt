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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.android.accessibility.talkback.R
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

/**
 * Lets the user choose the image that SandFly's wallpaper shows. The image is scaled down to the
 * screen and kept in SandFly's files, so the wallpaper needs no permission to read it.
 */
class WallpaperImageActivity : Activity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState == null) {
      val pick =
        Intent(Intent.ACTION_OPEN_DOCUMENT)
          .addCategory(Intent.CATEGORY_OPENABLE)
          .setType("image/*")
      try {
        @Suppress("DEPRECATION") startActivityForResult(pick, PICK)
      } catch (e: RuntimeException) {
        finish()
      }
    }
  }

  @Deprecated("Deprecated in Java")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    @Suppress("DEPRECATION") super.onActivityResult(requestCode, resultCode, data)
    val uri = data?.data
    if (requestCode != PICK || resultCode != RESULT_OK || uri == null) {
      finish()
      return
    }
    val app = applicationContext
    val width = resources.displayMetrics.widthPixels
    val height = resources.displayMetrics.heightPixels
    worker.execute {
      val saved = save(app, uri, maxOf(width, height))
      Handler(Looper.getMainLooper()).post {
        Toast.makeText(
            app,
            if (saved) R.string.wallpaper_image_set else R.string.wallpaper_image_failed,
            Toast.LENGTH_SHORT,
          )
          .show()
        if (saved) SandFlyWallpaperService.redraw()
      }
    }
    finish()
  }

  private companion object {
    const val TAG = "WallpaperImage"
    const val PICK = 1
    // Larger files are refused rather than read: an image this size is not a photo.
    const val MAX_BYTES = 64L * 1024 * 1024
    val worker = Executors.newSingleThreadExecutor()

    /**
     * Saves the image at [uri], scaled so that its longer side is about [longest] pixels, as the
     * wallpaper's image. Returns whether it did.
     */
    fun save(context: Context, uri: Uri, longest: Int): Boolean {
      val resolver = context.contentResolver
      return try {
        val size =
          resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: return false
        if (size > MAX_BYTES) return false
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= longest) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap =
          resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return false
        val target = SandFlyWallpaperService.imageFile(context)
        val temporary = File(target.parentFile, target.name + ".new")
        try {
          FileOutputStream(temporary).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
          bitmap.recycle()
        }
        temporary.renameTo(target)
      } catch (e: Throwable) {
        // A broken or huge image, or running out of memory, must never take SandFly down.
        LogUtils.e(TAG, "Cannot use the image as the wallpaper: %s", e)
        false
      }
    }
  }
}
