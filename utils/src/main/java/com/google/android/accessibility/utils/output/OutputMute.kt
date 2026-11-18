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

package com.google.android.accessibility.utils.output

import android.os.SystemClock
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.concurrent.atomic.AtomicInteger

/**
 * A short mute of SandFly's speech, sounds or vibration that a script starts with
 * `sf.feedback.mute`. Output while muted is dropped, not queued. A mute ends after its time, at
 * most [MAX_MS], when the script releases it, unloads or raises an error, when the app it started
 * in leaves the screen, and on any touch, gesture or key press of the user. A new mute replaces
 * the one before.
 */
object OutputMute {
  private const val TAG = "OutputMute"

  /** The longest a mute lasts. */
  const val MAX_MS = 10_000L

  private class Mute(
    val token: Int,
    val owner: String,
    val app: String?,
    val speech: Boolean,
    val sounds: Boolean,
    val vibration: Boolean,
    val until: Long,
  )

  @Volatile private var mute: Mute? = null
  private val tokens = AtomicInteger()
  /** Touches and keys a script is injecting, which are not the user's. */
  private val injecting = AtomicInteger()

  private fun active(): Mute? = mute?.takeIf { SystemClock.uptimeMillis() < it.until }

  @JvmStatic fun isSpeechMuted(): Boolean = active()?.speech == true

  @JvmStatic fun areSoundsMuted(): Boolean = active()?.sounds == true

  @JvmStatic fun isVibrationMuted(): Boolean = active()?.vibration == true

  /**
   * Mutes what is true for [ms], at most [MAX_MS], for [owner], a script, in [app], the package in
   * front. Returns the token that [release] takes.
   */
  @JvmStatic
  @Synchronized
  fun start(owner: String, app: String?, speech: Boolean, sounds: Boolean, vibration: Boolean, ms: Long): Int {
    val token = tokens.incrementAndGet()
    val length = ms.coerceIn(0L, MAX_MS)
    mute = Mute(token, owner, app, speech, sounds, vibration, SystemClock.uptimeMillis() + length)
    LogUtils.i(
      TAG,
      "Muted for %d ms by %s in %s: speech=%b sounds=%b vibration=%b",
      length,
      owner,
      app,
      speech,
      sounds,
      vibration,
    )
    return token
  }

  /** Ends the mute [token] started, if it is still the current one. */
  @JvmStatic
  @Synchronized
  fun release(token: Int) {
    if (mute?.token == token) end("released")
  }

  /** Ends a mute [owner] started. */
  @JvmStatic
  @Synchronized
  fun releaseOwner(owner: String, why: String) {
    if (mute?.owner == owner) end(why)
  }

  /** Ends any mute, as the user touched the screen, made a gesture or pressed a key. */
  @JvmStatic
  fun onUserInput(what: String) {
    if (mute == null || injecting.get() > 0) return
    synchronized(this) { if (mute != null) end(what) }
  }

  /** Ends a mute started in another app than [packageName], which is now in front. */
  @JvmStatic
  fun onAppChanged(packageName: String?) {
    val current = mute ?: return
    if (current.app != null && current.app != packageName) {
      synchronized(this) { if (mute === current) end("app left") }
    }
  }

  /** A script starts injecting a touch or keys, until [scriptInputEnded]. */
  @JvmStatic fun scriptInputStarted() {
    injecting.incrementAndGet()
  }

  @JvmStatic fun scriptInputEnded() {
    injecting.updateAndGet { maxOf(0, it - 1) }
  }

  private fun end(why: String) {
    val ended = mute ?: return
    mute = null
    if (SystemClock.uptimeMillis() < ended.until) {
      LogUtils.i(TAG, "Mute by %s ended: %s", ended.owner, why)
    }
  }
}
