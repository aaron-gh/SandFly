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

package com.google.android.accessibility.talkback.gesture

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import com.google.android.accessibility.talkback.shizuku.IShizukuGrants
import com.google.android.accessibility.talkback.shizuku.ShizukuShell
import com.google.android.accessibility.utils.AccessibilityWindowInfoUtils
import com.google.android.accessibility.utils.Performance.EventId
import com.google.android.accessibility.utils.input.WindowEventInterpreter
import com.google.android.accessibility.utils.input.WindowEventInterpreter.EventInterpretation
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.concurrent.Executors

/**
 * Sends a finger that SandFly explores on Gboard itself ([KeyboardExploration]) on to Gboard as
 * the hover events of touch exploration, injected through SandFly's shell service, Shizuku or root.
 * Gboard plays the feedback of its own settings, such as Vibrate on keypress, when a hover enters
 * its keys, and gets no hover while SandFly keeps the touch from touch exploration. Without Shizuku
 * or root, nothing is sent.
 *
 * Gboard reports the hover with hover events of its own, which SandFly drops: see [isSentHover].
 *
 * Everything here runs on the main thread, except the injections.
 */
object KeyboardHover {
  private const val TAG = "KeyboardHover"

  /** How long after the hover's exit Gboard's events for it can still be stamped. */
  private const val LATE_EVENT_MS = 150L

  /** How long the shell service has to connect before a touch starts it again. */
  private const val RECONNECT_AFTER_MS = 5_000L

  /** How far, in pixels, the finger moves before the hover moves with it. */
  private const val MOVE_STEP_PX = 6f

  private val mainHandler = Handler(Looper.getMainLooper())
  /** Injects in order, one event at a time. */
  private val executor =
    Executors.newSingleThreadExecutor { Thread(it, TAG).apply { isDaemon = true } }

  @Volatile private var shell: IShizukuGrants? = null
  private val connectListener: (IShizukuGrants) -> Unit = { shell = it }
  private var holdingShell = false
  /** When the shell service was last held, to start it again if it doesn't connect. */
  private var heldAtMs = 0L
  /** The handler following Gboard's window, which holds the shell service. */
  private var windowHandler: WindowHandler? = null

  private var hovering = false
  private var downTime = 0L
  private var lastX = 0f
  private var lastY = 0f

  /**
   * When the hover sent last started, and when it ended, updated once its exit was injected;
   * MAX_VALUE until then.
   */
  @Volatile private var hoverStartMs = Long.MAX_VALUE
  @Volatile private var hoverEndMs = Long.MAX_VALUE
  /** Counts the hovers, so that the exit of one injected late doesn't end the next. */
  @Volatile private var hoverCount = 0

  /**
   * Keeps the shell service running while Gboard's window is shown, so that a touch on it doesn't
   * wait for the service to start.
   */
  class WindowHandler(context: Context) : WindowEventInterpreter.WindowEventHandler {
    private val context = context.applicationContext
    private var shown = false

    init {
      windowHandler = this
    }

    override fun handle(interpretation: EventInterpretation, eventId: EventId?) {
      shown =
        interpretation.inputMethod.id != AccessibilityWindowInfoUtils.WINDOW_ID_NONE &&
          isGboardCurrent()
      update()
    }

    /**
     * Notes a touch on Gboard, whose window is then shown, even before a window event says so. A
     * shell service that stopped, as when Shizuku restarts, is started again.
     */
    fun onGboardTouched() {
      shown = true
      if (
        holdingShell &&
          shell == null &&
          SystemClock.uptimeMillis() - heldAtMs > RECONNECT_AFTER_MS
      ) {
        letGo()
      }
      update()
    }

    /** Holds the shell service while Gboard is shown and Shizuku or root allows SandFly. */
    private fun update() {
      if (shown && !holdingShell && ShizukuShell.isAvailable(context) && ShizukuShell.isAllowed()) {
        LogUtils.v(TAG, "Holding the shell service for Gboard")
        holdingShell = true
        heldAtMs = SystemClock.uptimeMillis()
        ShizukuShell.addConnectListener(connectListener)
        ShizukuShell.hold(context)
      } else if (!shown && holdingShell) {
        letGo()
      }
    }

    private fun letGo() {
      holdingShell = false
      ShizukuShell.removeConnectListener(connectListener)
      ShizukuShell.release()
      shell = null
    }

    fun shutdown() {
      if (windowHandler === this) {
        windowHandler = null
      }
      if (holdingShell) {
        letGo()
      }
    }

    private fun isGboardCurrent(): Boolean {
      val current =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
          ?: return false
      return KeyboardExploration.isGboardPackage(current.substringBefore('/'))
    }
  }

  /** Starts a hover at [x], [y], where the finger SandFly explores Gboard with is. */
  @JvmStatic
  fun enter(x: Float, y: Float) {
    if (shell == null) {
      // Shizuku may have allowed SandFly only after Gboard's window appeared: the next touch gets
      // the hover.
      windowHandler?.onGboardTouched()
      return
    }
    if (hovering) {
      exit()
    }
    LogUtils.v(TAG, "Hover entering at %.0f, %.0f", x, y)
    hovering = true
    hoverCount++
    downTime = SystemClock.uptimeMillis()
    hoverStartMs = downTime
    hoverEndMs = Long.MAX_VALUE
    lastX = x
    lastY = y
    inject(MotionEvent.ACTION_HOVER_ENTER, downTime, x, y, then = null)
  }

  /** Moves the hover with the finger, to [x], [y]. */
  @JvmStatic
  fun move(x: Float, y: Float) {
    if (!hovering || Math.hypot((x - lastX).toDouble(), (y - lastY).toDouble()) < MOVE_STEP_PX) {
      return
    }
    lastX = x
    lastY = y
    inject(MotionEvent.ACTION_HOVER_MOVE, downTime, x, y, then = null)
  }

  /** Ends the hover. */
  @JvmStatic
  fun exit() {
    if (!hovering) {
      return
    }
    hovering = false
    hoverEndMs = SystemClock.uptimeMillis()
    injectExit(then = null)
  }

  /**
   * Ends the hover, and runs [then] on the main thread once Gboard has handled its exit, or at once
   * without a hover. A hover exit on Gboard's popup of alternative characters types the character
   * under it, so a long press that opens the popup waits for this.
   */
  @JvmStatic
  fun exitThen(then: Runnable) {
    if (!hovering) {
      then.run()
      return
    }
    hovering = false
    hoverEndMs = SystemClock.uptimeMillis()
    injectExit(then)
  }

  /** Returns whether a hover is being sent to Gboard. */
  @JvmStatic fun isHovering(): Boolean = hovering

  /**
   * Returns whether [event] is one of Gboard's hover events for the hover sent to it, which isn't
   * the user's touch exploration.
   */
  @JvmStatic
  fun isSentHover(event: AccessibilityEvent): Boolean {
    val type = event.eventType
    if (
      type != AccessibilityEvent.TYPE_VIEW_HOVER_ENTER &&
        type != AccessibilityEvent.TYPE_VIEW_HOVER_EXIT
    ) {
      return false
    }
    val start = hoverStartMs
    val end = hoverEndMs
    val time = event.eventTime
    return time >= start &&
      (end == Long.MAX_VALUE || time <= end + LATE_EVENT_MS) &&
      KeyboardExploration.isGboardPackage(event.packageName)
  }

  private fun injectExit(then: Runnable?) {
    // Off the screen, so that Gboard cancels the key under the hover instead of lifting on it.
    inject(MotionEvent.ACTION_HOVER_EXIT, downTime, -1f, -1f, then)
  }

  private fun inject(action: Int, downTime: Long, x: Float, y: Float, then: Runnable?) {
    val target = shell
    val hover = hoverCount
    if (target == null) {
      then?.let { mainHandler.post(it) }
      return
    }
    executor.execute {
      try {
        val error = target.injectHover(action, downTime, x, y, then != null)
        if (error != null) {
          LogUtils.w(TAG, "Hover %s not sent: %s", MotionEvent.actionToString(action), error)
        }
      } catch (e: Exception) {
        // The service stopped, or is an older one without the call.
        LogUtils.w(TAG, "Hover %s not sent: %s", MotionEvent.actionToString(action), e)
        if (shell === target) {
          shell = null
        }
      } finally {
        if (action == MotionEvent.ACTION_HOVER_EXIT && hover == hoverCount) {
          hoverEndMs = SystemClock.uptimeMillis()
        }
        then?.let { mainHandler.post(it) }
      }
    }
  }
}
