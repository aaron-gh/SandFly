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

import com.google.android.accessibility.talkback.Pipeline

/**
 * What the service tells the SandFly tutorial, which runs in the service's process: each gesture
 * with its action, and when continuous reading starts and stops. All calls are on the main thread.
 */
object TutorialHooks {
  interface Listener {
    /**
     * A gesture assigned to [action] was made. Returns true if the tutorial takes the gesture, so
     * that the action isn't performed.
     */
    fun onGesture(gestureId: Int, action: String): Boolean

    fun onContinuousReading(active: Boolean)
  }

  /** SandFly's feedback, or null while SandFly is off. */
  @JvmStatic @Volatile var feedback: Pipeline.FeedbackReturner? = null

  /** The tutorial while it is on screen. */
  @JvmStatic var listener: Listener? = null

  @JvmStatic
  fun onGesture(gestureId: Int, action: String): Boolean =
    listener?.onGesture(gestureId, action) ?: false

  @JvmStatic
  fun onContinuousReading(active: Boolean) {
    listener?.onContinuousReading(active)
  }
}
