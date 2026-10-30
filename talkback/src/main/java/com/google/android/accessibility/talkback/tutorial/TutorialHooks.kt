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

package com.google.android.accessibility.talkback.tutorial

import android.view.KeyEvent
import com.google.android.accessibility.talkback.Pipeline

/**
 * What the service tells the SandFly tutorial, which runs in the service's process: each gesture
 * or keyboard shortcut with its command, Android's own keyboard shortcuts, browse mode, and when
 * continuous reading starts and stops. All calls are on the main thread.
 */
object TutorialHooks {
  interface Listener {
    /**
     * A gesture assigned to [action] was made. Returns true if the tutorial takes the gesture, so
     * that the action isn't performed.
     */
    fun onGesture(gestureId: Int, action: String): Boolean

    /**
     * Keys that run [command] were pressed, written as [keys] when known. Returns true if the
     * tutorial takes them, so that the command isn't run.
     */
    fun onKeys(keys: String?, command: String): Boolean

    /**
     * [keyCode] was pressed with the Meta key, one of Android's own shortcuts in
     * [SYSTEM_KEY_CODES]. Returns true if the tutorial takes it, so that Android doesn't act on it.
     */
    fun onSystemKeys(keyCode: Int): Boolean

    /** The user turned browse mode on or off. */
    fun onBrowseMode(enabled: Boolean)

    fun onContinuousReading(active: Boolean)
  }

  /** Keys that Android acts on with the Meta key: Back, Home, recent apps and notifications. */
  @JvmField
  val SYSTEM_KEY_CODES =
    setOf(KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_H, KeyEvent.KEYCODE_TAB, KeyEvent.KEYCODE_N)

  /** SandFly's feedback, or null while SandFly is off. */
  @JvmStatic @Volatile var feedback: Pipeline.FeedbackReturner? = null

  /** The tutorial while it is on screen. */
  @JvmStatic var listener: Listener? = null

  @JvmStatic
  fun onGesture(gestureId: Int, action: String): Boolean =
    listener?.onGesture(gestureId, action) ?: false

  @JvmStatic
  fun onKeys(keys: String?, command: String): Boolean = listener?.onKeys(keys, command) ?: false

  @JvmStatic fun onSystemKeys(keyCode: Int): Boolean = listener?.onSystemKeys(keyCode) ?: false

  @JvmStatic
  fun onBrowseMode(enabled: Boolean) {
    listener?.onBrowseMode(enabled)
  }

  /** Whether continuous reading is on, kept while no tutorial listens. */
  @JvmStatic @Volatile var continuousReading = false
    private set

  @JvmStatic
  fun onContinuousReading(active: Boolean) {
    continuousReading = active
    listener?.onContinuousReading(active)
  }
}
