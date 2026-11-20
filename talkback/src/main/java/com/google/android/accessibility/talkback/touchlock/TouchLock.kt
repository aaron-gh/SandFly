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
package com.google.android.accessibility.talkback.touchlock

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import androidx.annotation.MainThread
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.ProximitySensor
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.monitor.ScreenMonitor
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.concurrent.atomic.AtomicInteger

/**
 * Touch lock: while it is on, touches and gestures on the screen do nothing and nothing reaches the
 * app. Keys, volume key actions, fingerprint gestures and back tap still work. It is turned on by
 * the Lock or unlock touch command, by Lock touch automatically (when the screen turns on, on the
 * lock screen, or while the proximity sensor is covered), and for up to [MAX_SCRIPT_MS] by a script.
 * Pressing both volume keys together always unlocks it.
 *
 * Only SandFly's own gesture detection, from Android 13, sees touches before apps, so the lock
 * exists only where it is registered.
 */
object TouchLock {
  private const val TAG = "TouchLock"

  /** The longest a script locks touch. */
  const val MAX_SCRIPT_MS = 10_000L

  /** What SandFly does when a touch goes down while touch is locked. */
  enum class LockedTouchFeedback {
    NONE,
    SOUND,
    VIBRATION,
  }

  /** When touch locks without the command. */
  enum class Automatic(val value: String) {
    OFF("off"),
    SCREEN_ON("screen_on"),
    LOCK_SCREEN("lock_screen"),
    PROXIMITY("proximity");

    companion object {
      fun of(value: String?): Automatic = entries.firstOrNull { it.value == value } ?: OFF
    }
  }

  /** What touch lock needs from the service. */
  interface Host {
    /** Whether SandFly's own gesture detection is registered, so touches come to SandFly first. */
    fun isAvailable(): Boolean

    fun speak(text: CharSequence)

    /** Plays the feedback of a touch while touch is locked. */
    fun lockedTouch(feedback: LockedTouchFeedback)

    /** Called on the main thread when touch locks or unlocks. */
    fun onLockChanged(locked: Boolean)

    /** Called when both volume keys unlocked touch, so volume key actions forget the first key. */
    fun onVolumeKeysTaken()
  }

  private class ScriptLock(val token: Int, val owner: String, val app: String?, val until: Long)

  private val handler = Handler(Looper.getMainLooper())
  private val tokens = AtomicInteger()

  private var context: Context? = null
  private var host: Host? = null
  private var prefs: SharedPreferences? = null
  private var proximitySensor: ProximitySensor? = null
  private var automatic = Automatic.OFF
  private var screenOn = true

  /** Locked by the command, or by Automatic.SCREEN_ON. */
  @Volatile private var byUser = false
  @Volatile private var byLockScreen = false
  @Volatile private var byProximity = false
  @Volatile private var script: ScriptLock? = null
  private var applied = false

  private var volumeUpHeld = false
  private var volumeDownHeld = false
  private val swallowUps = mutableSetOf<Int>()

  private val scriptExpiry = Runnable { update("script time") }

  private val prefListener =
    SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (key == context?.getString(R.string.pref_lock_touch_automatically_key)) {
        reloadAutomatic()
      }
    }

  @JvmStatic
  @MainThread
  fun start(context: Context, host: Host) {
    this.context = context
    this.host = host
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    this.prefs = prefs
    prefs.registerOnSharedPreferenceChangeListener(prefListener)
    screenOn = true
    reloadAutomatic()
  }

  @JvmStatic
  @MainThread
  fun stop() {
    prefs?.unregisterOnSharedPreferenceChangeListener(prefListener)
    stopProximitySensor()
    handler.removeCallbacks(scriptExpiry)
    byUser = false
    byLockScreen = false
    byProximity = false
    script = null
    swallowUps.clear()
    volumeUpHeld = false
    volumeDownHeld = false
    update("SandFly stopped")
    host = null
    context = null
    prefs = null
  }

  private fun scriptLock(): ScriptLock? = script?.takeIf { SystemClock.uptimeMillis() < it.until }

  /** Whether touches and gestures on the screen are ignored now. */
  @JvmStatic
  fun isLocked(): Boolean = byUser || byLockScreen || byProximity || scriptLock() != null

  /** Whether touch can lock: only where SandFly's own gesture detection sees touches first. */
  private fun canLock(): Boolean = host?.isAvailable() == true

  /** A touch went down while touch is locked: gives the feedback chosen for it. */
  @JvmStatic
  @MainThread
  fun onLockedTouch() {
    val feedback =
      when (prefs?.getString(context?.getString(R.string.pref_locked_touch_feedback_key), null)) {
        context?.getString(R.string.locked_touch_feedback_sound_value) -> LockedTouchFeedback.SOUND
        context?.getString(R.string.locked_touch_feedback_vibration_value) ->
          LockedTouchFeedback.VIBRATION
        else -> LockedTouchFeedback.NONE
      }
    if (feedback != LockedTouchFeedback.NONE) host?.lockedTouch(feedback)
  }

  /** The Lock or unlock touch command. Returns whether it did either. */
  @JvmStatic
  @MainThread
  fun toggle(): Boolean {
    val host = host ?: return false
    val context = context ?: return false
    if (isLocked()) {
      unlock("command")
      return true
    }
    if (!canLock()) {
      host.speak(context.getString(R.string.touch_lock_unavailable))
      return false
    }
    byUser = true
    update("command")
    host.speak(context.getString(R.string.touch_locked))
    return true
  }

  /** Ends every lock, as the command or both volume keys do. */
  private fun unlock(why: String) {
    byUser = false
    byLockScreen = false
    byProximity = false
    script = null
    handler.removeCallbacks(scriptExpiry)
    update(why)
    context?.let { host?.speak(it.getString(R.string.touch_unlocked)) }
  }

  /**
   * Unlocks touch when both volume keys are pressed together while it is locked. Returns whether
   * [event] was used.
   */
  @JvmStatic
  @MainThread
  fun onKeyEvent(event: KeyEvent): Boolean {
    val keyCode = event.keyCode
    if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
      return false
    }
    val down = event.action == KeyEvent.ACTION_DOWN
    val otherHeld = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) volumeDownHeld else volumeUpHeld
    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) volumeUpHeld = down else volumeDownHeld = down
    if (!down) return swallowUps.remove(keyCode)
    if (event.repeatCount > 0) return swallowUps.contains(keyCode)
    if (!otherHeld || !isLocked()) return false
    swallowUps.clear()
    swallowUps += listOf(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN)
    host?.onVolumeKeysTaken()
    unlock("both volume keys")
    return true
  }

  /** The screen turned on or off. */
  @JvmStatic
  @MainThread
  fun onScreenChanged(on: Boolean) {
    screenOn = on
    if (on && canLock()) {
      when (automatic) {
        Automatic.SCREEN_ON -> byUser = true
        Automatic.LOCK_SCREEN -> byLockScreen = isKeyguardLocked()
        else -> {}
      }
    } else {
      byLockScreen = false
      byProximity = false
    }
    updateProximitySensor()
    update(if (on) "screen on" else "screen off")
  }

  /** The device was unlocked, so the lock screen's lock ends. */
  @JvmStatic
  @MainThread
  fun onDeviceUnlocked() {
    if (byLockScreen) {
      byLockScreen = false
      update("device unlocked")
    }
  }

  /**
   * Locks touch for [ms], at most [MAX_SCRIPT_MS], for [owner], a script, in [app], the package in
   * front. Returns the token that [release] takes. Replaces an earlier script lock.
   */
  @JvmStatic
  fun lockForScript(owner: String, app: String?, ms: Long): Int {
    val token = tokens.incrementAndGet()
    if (!canLock()) return token
    val length = ms.coerceIn(0L, MAX_SCRIPT_MS)
    script = ScriptLock(token, owner, app, SystemClock.uptimeMillis() + length)
    LogUtils.i(TAG, "Touch locked for %d ms by %s in %s", length, owner, app)
    handler.post {
      handler.removeCallbacks(scriptExpiry)
      handler.postDelayed(scriptExpiry, length)
      update("script")
    }
    return token
  }

  /** Ends the script lock [token] started, if it is still the current one. */
  @JvmStatic
  fun release(token: Int) {
    if (script?.token == token) endScriptLock("released")
  }

  /** Ends a script lock [owner] started. */
  @JvmStatic
  fun releaseOwner(owner: String, why: String) {
    if (script?.owner == owner) endScriptLock(why)
  }

  /** Ends a script lock started in another app than [packageName], which is now in front. */
  @JvmStatic
  fun onAppChanged(packageName: String?) {
    val current = script ?: return
    if (current.app != null && current.app != packageName) endScriptLock("app left")
  }

  private fun endScriptLock(why: String) {
    script = null
    handler.post { update(why) }
  }

  private fun reloadAutomatic() {
    val context = context ?: return
    automatic =
      Automatic.of(prefs?.getString(context.getString(R.string.pref_lock_touch_automatically_key), null))
    // A lock of the mode before ends; the new one starts when its moment comes, or now for the
    // lock screen.
    byLockScreen =
      automatic == Automatic.LOCK_SCREEN && screenOn && canLock() && isKeyguardLocked()
    byProximity = false
    updateProximitySensor()
    update("automatic lock changed")
  }

  private fun isKeyguardLocked(): Boolean = context?.let { ScreenMonitor.isDeviceLocked(it) } == true

  private fun updateProximitySensor() {
    val context = context
    if (context == null || automatic != Automatic.PROXIMITY || !screenOn) {
      stopProximitySensor()
      return
    }
    if (proximitySensor == null) {
      proximitySensor =
        ProximitySensor(context).apply {
          setProximityChangeListener { close ->
            handler.post {
              if (proximitySensor != null && byProximity != close && (!close || canLock())) {
                byProximity = close
                update(if (close) "proximity covered" else "proximity uncovered")
              }
            }
          }
          start()
        }
    }
  }

  private fun stopProximitySensor() {
    proximitySensor?.stop()
    proximitySensor = null
    byProximity = false
  }

  /** Tells the host when the lock state changed. */
  private fun update(why: String) {
    val locked = isLocked()
    if (locked == applied) return
    applied = locked
    LogUtils.i(TAG, "Touch %s: %s", if (locked) "locked" else "unlocked", why)
    host?.onLockChanged(locked)
  }
}
