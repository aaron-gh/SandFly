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
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.os.bundleOf
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.shizuku.ShizukuCommand
import com.google.android.accessibility.talkback.shizuku.ShizukuShell
import org.json.JSONArray

/** Touches the screen, presses keys and types text for scripts. */
class ScriptTouch(private val service: AccessibilityService, private val feedback: ScriptFeedback) {
  private val mainHandler = Handler(Looper.getMainLooper())
  // Gestures under way, counted on the main thread, which share the pass-through region.
  private var gestures = 0

  /**
   * Performs strokes on the screen. Each stroke is `{ points, start, duration }`: a list of
   * `{ x, y }` in screen pixels, followed in [duration] milliseconds from [start] milliseconds
   * after the gesture begins.
   */
  fun gesture(strokes: JSONArray?, done: (Boolean) -> Unit) {
    apiCheck(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      "Touching the screen needs Android 11 or later"
    }
    val list = strokes.items()
    apiCheck(list.size in 1..MAX_STROKES) { "A gesture has 1 to $MAX_STROKES strokes" }
    val builder = GestureDescription.Builder()
    for (item in list) {
      val stroke = item as? org.json.JSONObject ?: apiError("A stroke is { points, start, duration }")
      val points = stroke.optJSONArray("points").items()
      apiCheck(points.size in 1..MAX_POINTS) { "A stroke has 1 to $MAX_POINTS points" }
      val path = Path()
      points.forEachIndexed { i, point ->
        val p = point as? org.json.JSONObject ?: apiError("A point is { x, y }")
        val x = p.optDouble("x", Double.NaN).toFloat()
        val y = p.optDouble("y", Double.NaN).toFloat()
        apiCheck(x >= 0 && y >= 0) { "A point needs an x and a y of 0 or more" }
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
      }
      val start = stroke.optLong("start", 0L)
      val duration = stroke.optLong("duration", DEFAULT_DURATION_MS)
      apiCheck(start in 0..MAX_DURATION_MS && duration in 1..MAX_DURATION_MS) {
        "A stroke starts within and lasts at most $MAX_DURATION_MS milliseconds"
      }
      builder.addStroke(GestureDescription.StrokeDescription(path, start, duration))
    }
    val description = asApiError { builder.build() }
    mainHandler.post {
      // Touch exploration would take the strokes as SandFly gestures, so touch passes through to
      // apps until the gesture ends.
      if (gestures++ == 0) feedback.passThrough(true)
      val finish = { performed: Boolean ->
        if (--gestures == 0) feedback.passThrough(false)
        done(performed)
      }
      val callback =
        object : AccessibilityService.GestureResultCallback() {
          override fun onCompleted(gestureDescription: GestureDescription?) = finish(true)

          override fun onCancelled(gestureDescription: GestureDescription?) = finish(false)
        }
      // Android applies the pass-through region a moment later.
      mainHandler.postDelayed(
        { if (!service.dispatchGesture(description, callback, mainHandler)) finish(false) },
        PASS_THROUGH_WAIT_MS,
      )
    }
  }

  /** Presses keys, given as names such as `enter` or `KEYCODE_TAB`, through Shizuku. */
  fun pressKeys(names: List<String>, done: (Boolean) -> Unit) {
    apiCheck(names.size in 1..MAX_KEYS) { "pressKeys takes 1 to $MAX_KEYS keys" }
    val codes = names.map(::keyCodeName)
    requireShizuku("Pressing keys")
    ShizukuShell.run(service, ShizukuCommand.INPUT_KEYEVENT, codes.joinToString(" "), done)
  }

  /**
   * Types [text] where the cursor is in the edit field that has input focus. A password field, or
   * text with no field, goes through Shizuku as key presses.
   */
  fun type(text: String, focused: AccessibilityNodeInfoCompat?, done: (Boolean) -> Unit) {
    apiCheck(text.length <= MAX_TYPED) { "type takes at most $MAX_TYPED characters" }
    if (focused != null && focused.isEditable && !focused.isPassword) {
      return done(insert(focused, text))
    }
    requireShizuku("Typing outside an edit field, or into a password field,")
    typeWithShell(text, done)
  }

  /**
   * Types [text] through Shizuku's shell, which takes text without line breaks or tabs, up to
   * [ShizukuCommand.MAX_TEXT] characters at a time: those are sent as Enter and Tab key presses,
   * other control characters are left out, and the text goes in pieces, one after another.
   */
  private fun typeWithShell(text: String, done: (Boolean) -> Unit) {
    val steps = mutableListOf<Pair<ShizukuCommand, String>>()
    val piece = StringBuilder()
    fun flush() {
      piece.chunked(ShizukuCommand.MAX_TEXT).forEach { steps += ShizukuCommand.INPUT_TEXT to it }
      piece.clear()
    }
    for (c in text.replace("\r\n", "\n")) {
      when {
        c == '\n' || c == '\r' || c == '\t' -> {
          flush()
          steps += ShizukuCommand.INPUT_KEYEVENT to if (c == '\t') "KEYCODE_TAB" else "KEYCODE_ENTER"
        }
        !c.isISOControl() -> piece.append(c)
      }
    }
    flush()
    if (steps.isEmpty()) return done(true)
    fun run(index: Int) {
      if (index == steps.size) return done(true)
      val (command, argument) = steps[index]
      ShizukuShell.run(service, command, argument) { ok -> if (ok) run(index + 1) else done(false) }
    }
    run(0)
  }

  private fun insert(node: AccessibilityNodeInfoCompat, text: String): Boolean {
    // An empty field often has its hint as its text.
    val current = if (node.isShowingHintText) "" else node.text?.toString().orEmpty()
    val start = node.textSelectionStart.takeIf { it in 0..current.length } ?: current.length
    val end = node.textSelectionEnd.takeIf { it in start..current.length } ?: start
    val updated = current.substring(0, start) + text + current.substring(end)
    val cursor = start + text.length
    if (
      !node.performAction(
        AccessibilityNodeInfo.ACTION_SET_TEXT,
        bundleOf(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE to updated),
      )
    ) {
      return false
    }
    // Some fields, such as search boxes, put the cursor at the end themselves and refuse this.
    node.performAction(
      AccessibilityNodeInfo.ACTION_SET_SELECTION,
      bundleOf(
        AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT to cursor,
        AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT to cursor,
      ),
    )
    return true
  }

  private fun requireShizuku(what: String) =
    apiCheck(ShizukuShell.isAllowed()) {
      "$what needs Shizuku running and allowed for SandFly"
    }

  private fun keyCodeName(name: String): String {
    val upper = name.trim().uppercase()
    val full = if (upper.startsWith("KEYCODE_")) upper else "KEYCODE_" + (KEY_ALIASES[upper] ?: upper)
    apiCheck(KeyEvent.keyCodeFromString(full) != KeyEvent.KEYCODE_UNKNOWN) { "Unknown key $name" }
    return full
  }

  private companion object {
    const val MAX_STROKES = 10
    const val MAX_POINTS = 100
    const val MAX_KEYS = 100
    const val MAX_TYPED = 2000
    const val DEFAULT_DURATION_MS = 50L
    const val MAX_DURATION_MS = 10_000L
    const val PASS_THROUGH_WAIT_MS = 350L

    val KEY_ALIASES =
      mapOf(
        "UP" to "DPAD_UP",
        "DOWN" to "DPAD_DOWN",
        "LEFT" to "DPAD_LEFT",
        "RIGHT" to "DPAD_RIGHT",
        "CENTER" to "DPAD_CENTER",
        "BACKSPACE" to "DEL",
        "DELETE" to "FORWARD_DEL",
        "ESC" to "ESCAPE",
        "RETURN" to "ENTER",
        "HOME" to "MOVE_HOME",
        "END" to "MOVE_END",
        "PAGEUP" to "PAGE_UP",
        "PAGEDOWN" to "PAGE_DOWN",
        "PLAYPAUSE" to "MEDIA_PLAY_PAUSE",
        "NEXT" to "MEDIA_NEXT",
        "PREVIOUS" to "MEDIA_PREVIOUS",
        "VOLUMEUP" to "VOLUME_UP",
        "VOLUMEDOWN" to "VOLUME_DOWN",
      )
  }
}
