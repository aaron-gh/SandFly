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

package com.google.android.accessibility.talkback.scripting

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Display
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.libraries.accessibility.utils.log.LogUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executor
import org.json.JSONArray
import org.json.JSONObject

/** Windows, items at a point, and text in pictures on the screen, for scripts. */
class ScriptScreen(private val service: AccessibilityService) {
  private val mainHandler = Handler(Looper.getMainLooper())
  private val mainExecutor = Executor { mainHandler.post(it) }
  private val recognizerHolder = lazy {
    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
  }
  private val recognizer by recognizerHolder

  /**
   * Every window on screen, top first, with a snapshot of its top item. SandFly's own windows, such
   * as its menu, are left out, as scripts change nothing in SandFly.
   */
  fun windows(snapshot: (AccessibilityNodeInfoCompat?) -> Any): JSONArray =
    JSONArray(
      appWindows()
        .sortedByDescending { it.layer }
        .map { window ->
          val bounds = Rect().also(window::getBoundsInScreen)
          jsonObject(
            "id" to window.id,
            "type" to WINDOW_TYPES[window.type],
            "title" to window.title?.toString(),
            "layer" to window.layer,
            "active" to window.isActive,
            "focused" to window.isFocused,
            "bounds" to boundsJson(bounds),
            "root" to snapshot(window.root?.let(AccessibilityNodeInfoCompat::wrap)),
          )
        }
    )

  /**
   * The innermost visible item at [x], [y], in the top window there. Stops at [deadline], a
   * [SystemClock.uptimeMillis] time, with the innermost item found so far.
   */
  fun itemAt(x: Int, y: Int, deadline: Long): AccessibilityNodeInfoCompat? {
    val window =
      appWindows()
        .filter { Rect().also(it::getBoundsInScreen).contains(x, y) }
        .maxByOrNull { it.layer } ?: return null
    var found = window.root?.let(AccessibilityNodeInfoCompat::wrap) ?: return null
    repeat(MAX_DEPTH) {
      if (SystemClock.uptimeMillis() > deadline) return found
      // Later children are drawn over earlier ones.
      val next =
        found.children().asReversed().firstOrNull {
          it.isVisibleToUser && Rect().also(it::getBoundsInScreen).contains(x, y)
        } ?: return found
      found = next
    }
    return found
  }

  private fun appWindows(): List<AccessibilityWindowInfo> =
    service.windows.filter { it.root?.packageName?.toString() != service.packageName }

  /**
   * Recognizes text in the part of the screen within [bounds], or all of it. [done] gets the text
   * and its blocks, or null and why it failed.
   */
  fun recognizeText(bounds: Rect?, done: (JSONObject?, String?) -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      return done(null, "Recognizing text needs Android 11 or later")
    }
    mainHandler.post { capture(bounds, retries = 2, done) }
  }

  private fun capture(bounds: Rect?, retries: Int, done: (JSONObject?, String?) -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    service.takeScreenshot(
      Display.DEFAULT_DISPLAY,
      mainExecutor,
      object : AccessibilityService.TakeScreenshotCallback {
        override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
          val buffer = result.hardwareBuffer
          val bitmap =
            try {
              Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)?.let { cropped(it, bounds) }
            } catch (e: RuntimeException) {
              LogUtils.w(TAG, "Could not crop the screenshot: %s", e)
              null
            } finally {
              buffer.close()
            }
          if (bitmap == null) return done(null, "The item is not on screen")
          recognizer
            .process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { text -> done(textJson(text, bounds), null) }
            .addOnFailureListener { e ->
              LogUtils.w(TAG, "Text recognition failed: %s", e)
              // Such as Google Play services still downloading the recognizer.
              done(null, "Text recognition failed: ${e.message}")
            }
            .addOnCompleteListener { bitmap.recycle() }
        }

        override fun onFailure(errorCode: Int) {
          // Android allows only a few screenshots a second.
          if (errorCode == AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT &&
            retries > 0) {
            mainHandler.postDelayed({ capture(bounds, retries - 1, done) }, SCREENSHOT_RETRY_MS)
          } else {
            done(null, "Android could not take a screenshot ($errorCode)")
          }
        }
      },
    )
  }

  /** A software copy of [screenshot] cut to [bounds], or null if nothing of it is on screen. */
  private fun cropped(screenshot: Bitmap, bounds: Rect?): Bitmap? {
    val area = Rect(0, 0, screenshot.width, screenshot.height)
    val software =
      try {
        // A collapsed item, of no width or height, still intersects the screen.
        if (bounds != null && (!area.intersect(bounds) || area.isEmpty)) return null
        screenshot.copy(Bitmap.Config.ARGB_8888, false) ?: return null
      } finally {
        screenshot.recycle()
      }
    if (area.width() == software.width && area.height() == software.height) return software
    return Bitmap.createBitmap(software, area.left, area.top, area.width(), area.height()).also {
      if (it !== software) software.recycle()
    }
  }

  // Recognized bounds are in the cropped picture, so they move back to screen coordinates.
  private fun textJson(text: Text, bounds: Rect?): JSONObject {
    val left = bounds?.left?.coerceAtLeast(0) ?: 0
    val top = bounds?.top?.coerceAtLeast(0) ?: 0
    fun place(box: Rect?) = box?.let { boundsJson(Rect(it).apply { offset(left, top) }) }
    return jsonObject(
      "text" to text.text,
      "blocks" to
        JSONArray(
          text.textBlocks.map { block ->
            jsonObject(
              "text" to block.text,
              "bounds" to place(block.boundingBox),
              "lines" to
                JSONArray(
                  block.lines.map { line ->
                    jsonObject("text" to line.text, "bounds" to place(line.boundingBox))
                  }
                ),
            )
          }
        ),
    )
  }

  fun close() {
    if (recognizerHolder.isInitialized()) recognizer.close()
  }

  companion object {
    private const val TAG = "ScriptScreen"
    private const val MAX_DEPTH = 60
    private const val SCREENSHOT_RETRY_MS = 400L

    fun boundsJson(bounds: Rect): JSONObject =
      jsonObject(
        "left" to bounds.left,
        "top" to bounds.top,
        "right" to bounds.right,
        "bottom" to bounds.bottom,
      )

    private val WINDOW_TYPES =
      mapOf(
        AccessibilityWindowInfo.TYPE_APPLICATION to "application",
        AccessibilityWindowInfo.TYPE_INPUT_METHOD to "inputMethod",
        AccessibilityWindowInfo.TYPE_SYSTEM to "system",
        AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY to "accessibilityOverlay",
        AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER to "splitScreenDivider",
        AccessibilityWindowInfo.TYPE_MAGNIFICATION_OVERLAY to "magnificationOverlay",
      )
  }
}
