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

package com.google.android.accessibility.talkback.actor

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.content.FileProvider
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.BuildConfig
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.utils.FocusIndicatorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.screencapture.ScreenshotCapture
import com.google.android.accessibility.utils.screencapture.ScreenshotCapture.WindowOrFullScreenScreenshotCallback
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.function.Consumer

/**
 * Share image of focused item and Share screenshot: takes a screenshot of the screen, crops it to
 * the focused item for the first, saves it as a PNG file in the cache and opens the Android share
 * sheet with it. Only the latest [KEPT_FILES] files are kept.
 *
 * Secure windows, such as some banking apps, are black in the screenshot.
 */
object ImageSharer {
  private const val TAG = "ImageSharer"

  /** The cache directory of the files, as file_paths.xml shares it. */
  @VisibleForTesting const val DIRECTORY = "shared_images"

  /** The files kept, so that an app that still reads an earlier one can finish. */
  @VisibleForTesting const val KEPT_FILES = 3

  private const val FILE_PREFIX = "SandFly-"
  private const val FILE_SUFFIX = ".png"
  private const val AUTHORITY = BuildConfig.APPLICATION_ID + ".providers.FileProvider"

  // How long the app on screen gets to redraw without the focus indicator.
  private const val REDRAW_MS = 150L
  // Android allows only a few screenshots a second.
  private const val RETRY_MS = 400L
  private const val RETRIES = 2
  // How long the screenshot may take before the request is given up.
  private const val TIMEOUT_MS = 3000L

  private val handler by lazy { Handler(Looper.getMainLooper()) }

  // The request in progress, until its screenshot arrives or it times out.
  private var current: Request? = null

  private class Request(
    val service: AccessibilityService,
    val bounds: Rect?,
    val focusHidden: Boolean,
    val speak: Consumer<String>,
  ) {
    val timeout = Runnable { if (current === this) fail(this) }
  }

  /**
   * Shares a screenshot of [node], or of the whole screen when [wholeScreen] is true. [speak] says
   * why it can't. Returns false when nothing is shared.
   */
  @JvmStatic
  fun share(
    service: AccessibilityService,
    node: AccessibilityNodeInfoCompat?,
    wholeScreen: Boolean,
    speak: Consumer<String>,
  ): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      speak.accept(service.getString(R.string.share_image_failed))
      return false
    }
    var bounds: Rect? = null
    if (!wholeScreen) {
      if (node == null) {
        speak.accept(service.getString(R.string.continuous_copy_no_focus))
        return false
      }
      bounds = Rect().also { node.getBoundsInScreen(it) }
      if (bounds.isEmpty) {
        speak.accept(service.getString(R.string.share_image_failed))
        return false
      }
    }
    if (current != null) {
      // The previous request is still taking its screenshot.
      return true
    }
    val hidden = FocusIndicatorUtils.hideForScreenshot(service)
    if (!hidden && bounds != null) {
      // The focus indicator is left out of the image.
      val stroke =
        FocusIndicatorUtils.getTalkBackFocusStrokeWidth(
          SharedPreferencesUtils.getSharedPreferences(service),
          service.resources,
        )
      bounds.inset(stroke, stroke)
    }
    val request = Request(service, bounds, hidden, speak)
    current = request
    handler.postDelayed(request.timeout, TIMEOUT_MS)
    if (hidden) {
      handler.postDelayed({ if (current === request) captureSupported(request) }, REDRAW_MS)
    } else {
      captureSupported(request)
    }
    return true
  }

  private fun captureSupported(request: Request) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      capture(request, RETRIES)
    }
  }

  @RequiresApi(Build.VERSION_CODES.R)
  private fun capture(request: Request, retries: Int) {
    ScreenshotCapture.takeScreenshotByNode(
      request.service,
      /* node= */ null,
      object : WindowOrFullScreenScreenshotCallback() {
        override fun onFailure(errorCode: Int) {
          super.onFailure(errorCode)
          if (current !== request) return
          if (
            errorCode == AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT &&
              retries > 0
          ) {
            handler.postDelayed({ if (current === request) capture(request, retries - 1) }, RETRY_MS)
          } else {
            fail(request)
          }
        }

        override fun onSuccess(screenshot: AccessibilityService.ScreenshotResult) {
          super.onSuccess(screenshot)
          val bitmap = bitmap
          if (current !== request) {
            bitmap?.recycle()
            return
          }
          if (bitmap == null) {
            fail(request)
            return
          }
          end(request)
          save(request, bitmap)
        }
      },
    )
  }

  private fun end(request: Request) {
    current = null
    handler.removeCallbacks(request.timeout)
    if (request.focusHidden) {
      FocusIndicatorUtils.restoreAfterScreenshot(request.service)
    }
  }

  private fun fail(request: Request) {
    end(request)
    request.speak.accept(request.service.getString(R.string.share_image_failed))
  }

  /** Writes the file on its own thread, as PNG compression takes a while, and then shares it. */
  private fun save(request: Request, bitmap: Bitmap) {
    val context = request.service.applicationContext
    Thread(
        {
          val uri =
            try {
              write(context, bitmap, request.bounds)
            } finally {
              bitmap.recycle()
            }
          handler.post {
            if (uri == null) {
              request.speak.accept(context.getString(R.string.share_image_failed))
            } else {
              open(request, uri)
            }
          }
        },
        TAG,
      )
      .start()
  }

  private fun write(context: Context, screenshot: Bitmap, bounds: Rect?): Uri? {
    val dir = File(context.cacheDir, DIRECTORY)
    if (!dir.isDirectory && !dir.mkdirs()) {
      LogUtils.w(TAG, "Could not make the directory for shared images")
      return null
    }
    val file = File(dir, fileName(System.currentTimeMillis(), TimeZone.getDefault()))
    var image: Bitmap? = null
    try {
      val cropped = crop(screenshot, bounds) ?: return null
      image = cropped
      FileOutputStream(file).use { out ->
        if (!cropped.compress(Bitmap.CompressFormat.PNG, /* quality= */ 100, out)) {
          throw IOException("PNG compression failed")
        }
      }
      for (old in oldFiles(dir.listFiles())) {
        old.delete()
      }
      return FileProvider.getUriForFile(context, AUTHORITY, file)
    } catch (e: IOException) {
      LogUtils.w(TAG, "Could not save the shared image: %s", e)
    } catch (e: IllegalArgumentException) {
      LogUtils.w(TAG, "Could not share the image file: %s", e)
    } catch (e: OutOfMemoryError) {
      LogUtils.w(TAG, "No memory for the shared image")
    } finally {
      if (image != null && image !== screenshot) {
        image.recycle()
      }
    }
    file.delete()
    return null
  }

  /** Returns the part of [screenshot] inside [bounds], all of it for null bounds. */
  private fun crop(screenshot: Bitmap, bounds: Rect?): Bitmap? {
    if (bounds == null) return screenshot
    val inside = Rect(bounds)
    if (!inside.intersect(0, 0, screenshot.width, screenshot.height)) {
      LogUtils.w(TAG, "The focused item is outside the screenshot")
      return null
    }
    return Bitmap.createBitmap(
      screenshot,
      inside.left,
      inside.top,
      inside.width(),
      inside.height(),
    )
  }

  private fun open(request: Request, uri: Uri) {
    val send =
      Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri("", uri)
    val chooser =
      Intent.createChooser(send, /* title= */ null)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
      request.service.startActivity(chooser)
    } catch (e: ActivityNotFoundException) {
      LogUtils.w(TAG, "No share sheet: %s", e)
      request.speak.accept(request.service.getString(R.string.share_image_failed))
    }
  }

  /** Returns the name of a file saved at [timeMillis], which sorts by time. */
  @VisibleForTesting
  fun fileName(timeMillis: Long, timeZone: TimeZone): String {
    val format = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US)
    format.timeZone = timeZone
    return FILE_PREFIX + format.format(Date(timeMillis)) + FILE_SUFFIX
  }

  /** Returns the shared image files in [files] beyond the latest [KEPT_FILES]. */
  @VisibleForTesting
  fun oldFiles(files: Array<File>?): List<File> =
    files
      .orEmpty()
      .filter { it.isFile && it.name.startsWith(FILE_PREFIX) && it.name.endsWith(FILE_SUFFIX) }
      .sortedByDescending { it.name }
      .drop(KEPT_FILES)
}
