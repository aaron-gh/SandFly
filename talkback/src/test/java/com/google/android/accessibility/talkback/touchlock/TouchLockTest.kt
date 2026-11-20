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
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class TouchLockTest {
  private lateinit var context: Context
  private val spoken = mutableListOf<String>()
  private val changes = mutableListOf<Boolean>()
  private val feedback = mutableListOf<TouchLock.LockedTouchFeedback>()
  private var available = true
  private var volumeKeysTaken = 0

  private val host =
    object : TouchLock.Host {
      override fun isAvailable() = available

      override fun speak(text: CharSequence) {
        spoken += text.toString()
      }

      override fun lockedTouch(feedback: TouchLock.LockedTouchFeedback) {
        this@TouchLockTest.feedback += feedback
      }

      override fun onLockChanged(locked: Boolean) {
        changes += locked
      }

      override fun onVolumeKeysTaken() {
        volumeKeysTaken++
      }
    }

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    SharedPreferencesUtils.getSharedPreferences(context).edit().clear().commit()
    TouchLock.start(context, host)
  }

  @After
  fun tearDown() {
    TouchLock.stop()
  }

  private fun idle() = shadowOf(Looper.getMainLooper()).idle()

  private fun key(keyCode: Int, action: Int): Boolean {
    val now = SystemClock.uptimeMillis()
    return TouchLock.onKeyEvent(KeyEvent(now, now, action, keyCode, 0))
  }

  @Test
  fun commandLocksAndUnlocks() {
    assertTrue(TouchLock.toggle())
    assertTrue(TouchLock.isLocked())
    assertTrue(TouchLock.toggle())
    assertFalse(TouchLock.isLocked())
    assertEquals(
      listOf(context.getString(R.string.touch_locked), context.getString(R.string.touch_unlocked)),
      spoken,
    )
    assertEquals(listOf(true, false), changes)
  }

  @Test
  fun nothingLocksWithoutSandFlysGestureDetection() {
    available = false
    assertFalse(TouchLock.toggle())
    assertFalse(TouchLock.isLocked())
    assertEquals(listOf(context.getString(R.string.touch_lock_unavailable)), spoken)
  }

  @Test
  fun bothVolumeKeysUnlock() {
    // Unlocked, both volume keys go on as they would.
    assertFalse(key(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN))
    assertFalse(key(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN))
    assertFalse(key(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP))
    assertFalse(key(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_UP))

    TouchLock.toggle()
    // One volume key alone changes the volume.
    assertFalse(key(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN))
    assertFalse(key(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_UP))
    assertTrue(TouchLock.isLocked())

    assertFalse(key(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN))
    assertTrue(key(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN))
    assertFalse(TouchLock.isLocked())
    assertEquals(1, volumeKeysTaken)
    // Both releases are taken too.
    assertTrue(key(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP))
    assertTrue(key(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_UP))
    assertEquals(context.getString(R.string.touch_unlocked), spoken.last())
  }

  @Test
  fun scriptLockEndsAfterItsTimeAtMostTenSeconds() {
    TouchLock.lockForScript("script", "app", 60_000)
    idle()
    assertTrue(TouchLock.isLocked())
    assertEquals(listOf(true), changes)
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(TouchLock.MAX_SCRIPT_MS + 1))
    assertFalse(TouchLock.isLocked())
    assertEquals(listOf(true, false), changes)
  }

  @Test
  fun scriptLockEndsWithItsTokenOwnerOrApp() {
    val token = TouchLock.lockForScript("a", "app", 5_000)
    TouchLock.release(token + 1)
    assertTrue(TouchLock.isLocked())
    TouchLock.release(token)
    assertFalse(TouchLock.isLocked())

    TouchLock.lockForScript("a", "app", 5_000)
    TouchLock.releaseOwner("b", "other script")
    assertTrue(TouchLock.isLocked())
    TouchLock.releaseOwner("a", "unloaded")
    assertFalse(TouchLock.isLocked())

    TouchLock.lockForScript("a", "app", 5_000)
    TouchLock.onAppChanged("app")
    assertTrue(TouchLock.isLocked())
    TouchLock.onAppChanged("another")
    assertFalse(TouchLock.isLocked())
  }

  @Test
  fun screenOnLocksWhenChosen() {
    TouchLock.onScreenChanged(false)
    TouchLock.onScreenChanged(true)
    assertFalse(TouchLock.isLocked())
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putString(context.getString(R.string.pref_lock_touch_automatically_key), "screen_on")
      .commit()
    // Not at once: the next time the screen turns on.
    assertFalse(TouchLock.isLocked())
    TouchLock.onScreenChanged(false)
    TouchLock.onScreenChanged(true)
    assertTrue(TouchLock.isLocked())
    // No speech for an automatic lock.
    assertTrue(spoken.isEmpty())
  }

  @Test
  fun lockedTouchGivesTheChosenFeedback() {
    TouchLock.onLockedTouch()
    assertTrue(feedback.isEmpty())
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putString(
        context.getString(R.string.pref_locked_touch_feedback_key),
        context.getString(R.string.locked_touch_feedback_vibration_value),
      )
      .commit()
    TouchLock.onLockedTouch()
    assertEquals(listOf(TouchLock.LockedTouchFeedback.VIBRATION), feedback)
  }
}
