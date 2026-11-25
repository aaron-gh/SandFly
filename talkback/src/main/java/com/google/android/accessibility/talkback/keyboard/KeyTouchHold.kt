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
package com.google.android.accessibility.talkback.keyboard

import android.content.Context
import android.graphics.Rect
import android.os.SystemClock
import android.view.Display
import android.view.KeyEvent
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.macros.PassThroughTouch
import com.google.android.accessibility.utils.AccessibilityWindowInfoUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Hold a touch on the focused item: a touch held down on the item with SandFly focus, passed
 * through to the app, for buttons that act while a finger stays on them, such as the voice message
 * button of Telegram, which records while the touch is down and sends the message when it lifts.
 *
 * Run by a key press, the touch stays down while the key is held and lifts when it is released.
 * When the key is released within [LATCH_MS], or the command is run by a gesture or a menu, the
 * touch is latched: it stays down until the command runs again or Back or Escape is pressed.
 * "Touch held" is spoken when the touch latches and "Touch lifted" when a latched touch lifts.
 * Every touch lifts after [MAX_HOLD_MS], when its item is no longer on the screen, and when SandFly
 * is suspended or stops.
 */
class KeyTouchHold(private val context: Context, private val speaker: (String) -> Unit) {

  /** What holds the touch. */
  fun interface Toucher {
    /**
     * Puts a touch down on the item with SandFly focus and holds it until [stopped] turns true or
     * the item is gone, then lifts it and calls [ended]. Returns whether the touch started.
     */
    fun start(stopped: () -> Boolean, ended: () -> Unit): Start
  }

  /** Whether a touch started, or why not. */
  enum class Start {
    STARTED,
    NO_FOCUS,
    UNAVAILABLE,
  }

  /** A touch held now. */
  private class Hold(val startTime: Long) {
    /** The key whose release lifts or latches the touch, or none once released or latched. */
    var keyCode = KeyEvent.KEYCODE_UNKNOWN
    var keyDownTime = 0L
    var latched = false
    /** Whether the touch is to lift, at the end of the part of it under way. */
    var lifting = false
    /** Whether the lift was already spoken, or isn't spoken. */
    var quiet = false
  }

  /** What holds the touch; by default nothing, as in tests and outside the service. */
  var toucher: Toucher = Toucher { _, _ -> Start.UNAVAILABLE }

  private var hold: Hold? = null

  /** Back and Escape presses that lifted a touch, whose repeats and release are taken too. */
  private val taken = mutableSetOf<Int>()

  /** Whether a touch is held, latched or not. */
  val isHeld: Boolean
    get() = hold != null

  /** Whether a touch is held and latched. */
  val isLatched: Boolean
    get() = hold?.latched == true

  /**
   * Runs the command: lifts the touch held, or holds a touch on the focused item. [event] is the
   * key press that runs it, or null for gestures and menus; a repeat of a key held down does
   * nothing.
   */
  fun toggle(event: KeyEvent?) {
    if (event != null && event.action == KeyEvent.ACTION_DOWN && event.repeatCount > 0) return
    val current = hold
    if (current != null) {
      lift(current, spoken = true)
      return
    }
    start(event)
  }

  private fun start(event: KeyEvent?) {
    val started = Hold(SystemClock.uptimeMillis())
    hold = started
    val result =
      toucher.start(
        stopped = {
          started.lifting || SystemClock.uptimeMillis() - started.startTime >= MAX_HOLD_MS
        },
        ended = { onEnded(started) },
      )
    if (result != Start.STARTED) {
      if (hold === started) hold = null
      LogUtils.i(TAG, "Touch not held: %s", result)
      speaker(
        context.getString(
          if (result == Start.NO_FOCUS) R.string.touch_hold_no_focus
          else R.string.touch_hold_unavailable
        )
      )
      return
    }
    if (event != null && event.action == KeyEvent.ACTION_DOWN) {
      started.keyCode = event.keyCode
      started.keyDownTime = event.eventTime
      LogUtils.i(TAG, "Touch held while %s is down", KeyEvent.keyCodeToString(event.keyCode))
    } else {
      latch(started)
    }
  }

  /**
   * Returns whether [event] is the touch hold's, so that nothing else gets it: repeats of the key
   * holding the touch, and Back or Escape without modifiers, which lift it. The release of the key
   * holding the touch lifts or latches it and goes on, so that the keymap sees it. [modifiers] are
   * the modifiers of [event], with the SandFly key's.
   */
  fun onKeyEvent(event: KeyEvent, modifiers: Int): Boolean {
    val keyCode = event.keyCode
    if (keyCode in taken) {
      if (event.action == KeyEvent.ACTION_UP) taken -= keyCode
      return true
    }
    val current = hold ?: return false
    if (current.keyCode != KeyEvent.KEYCODE_UNKNOWN && keyCode == current.keyCode) {
      return when (event.action) {
        KeyEvent.ACTION_DOWN -> event.repeatCount > 0
        KeyEvent.ACTION_UP -> {
          current.keyCode = KeyEvent.KEYCODE_UNKNOWN
          if (event.eventTime - current.keyDownTime < LATCH_MS) {
            latch(current)
          } else {
            lift(current, spoken = false)
          }
          false
        }
        else -> true
      }
    }
    if (
      event.action == KeyEvent.ACTION_DOWN &&
        event.repeatCount == 0 &&
        (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) &&
        modifiers == KeyComboModel.NO_MODIFIER
    ) {
      taken += keyCode
      lift(current, spoken = true)
      return true
    }
    return false
  }

  /** Lifts the touch held without a word, as SandFly is suspended or stops. */
  fun reset() {
    hold?.let { lift(it, spoken = false) }
    taken.clear()
  }

  private fun latch(current: Hold) {
    current.latched = true
    LogUtils.i(TAG, "Touch latched")
    speaker(context.getString(R.string.touch_hold_held))
  }

  private fun lift(current: Hold, spoken: Boolean) {
    if (hold === current) hold = null
    current.lifting = true
    current.quiet = true
    LogUtils.i(TAG, "Touch lifting")
    if (spoken) speaker(context.getString(R.string.touch_hold_lifted))
  }

  /** The touch lifted: at the maximum, when its item went, or when another touch cancelled it. */
  private fun onEnded(ended: Hold) {
    if (hold !== ended) return
    hold = null
    LogUtils.i(TAG, "Touch lifted")
    if (!ended.quiet) speaker(context.getString(R.string.touch_hold_lifted))
  }

  /**
   * Holds the touch on the centre of the part on the screen of the item that [focus] gives, the
   * item with SandFly focus, through [PassThroughTouch]. The touch lifts when the item is no longer
   * in the app's window, which is checked every [ITEM_CHECK_MS].
   */
  class FocusToucher(
    private val service: TalkBackService,
    private val focus: () -> AccessibilityNodeInfoCompat?,
  ) : Toucher {
    override fun start(stopped: () -> Boolean, ended: () -> Unit): Start {
      if (!PassThroughTouch.isSupported() || PassThroughTouch.isLockedElsewhere(service)) {
        return Start.UNAVAILABLE
      }
      val node = focus() ?: return Start.NO_FOCUS
      if (AccessibilityWindowInfoUtils.getDisplayId(node.window) != Display.DEFAULT_DISPLAY) {
        return Start.UNAVAILABLE
      }
      val bounds = Rect()
      node.getBoundsInScreen(bounds)
      val centre = PassThroughTouch.visibleCentre(service, bounds) ?: return Start.UNAVAILABLE
      var nextCheck = SystemClock.uptimeMillis() + ITEM_CHECK_MS
      var gone = false
      val itemGone = {
        val now = SystemClock.uptimeMillis()
        if (!gone && now >= nextCheck) {
          nextCheck = now + ITEM_CHECK_MS
          gone = !node.refresh()
          if (gone) LogUtils.i(TAG, "Item gone; touch lifting")
        }
        gone
      }
      val held = PassThroughTouch.hold(service, centre, { stopped() || itemGone() }) { ended() }
      return if (held) Start.STARTED else Start.UNAVAILABLE
    }
  }

  companion object {
    private const val TAG = "KeyTouchHold"

    /** A key released sooner than this after it went down latches the touch. */
    const val LATCH_MS = 500L

    /** The longest a touch is held: 10 minutes. */
    const val MAX_HOLD_MS = 10 * 60 * 1000L

    /** How often a touch held checks that its item is still in the app's window. */
    const val ITEM_CHECK_MS = 1_000L
  }
}
