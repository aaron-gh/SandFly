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

package com.google.android.accessibility.talkback.trainer

import android.view.MotionEvent
import com.google.android.accessibility.talkback.gesture.EdgeSwipeTracker.MenuEdge

/**
 * What the service tells the gesture trainer, which runs in the service's process: every touch on
 * the screen and every gesture recognized, while the trainer waits for attempts. All calls are on
 * the main thread.
 */
object GestureTrainerHooks {
  interface Listener {
    /** The gesture being practised, which is recognized as if it had a command. */
    val practisedGestureId: Int

    fun onTouch(event: MotionEvent)

    /** The touch interaction ended, also one whose motion events stopped coming to the service. */
    fun onTouchInteractionEnded(timeMs: Long, explored: Boolean) {}

    /** Returns true to take the gesture, so that its command isn't performed. */
    fun onGesture(gestureId: Int): Boolean

    /** Names of sample items for practising the menu along [edge] when it has none, or null. */
    fun practiceMenuItems(edge: MenuEdge): List<String>? = null

    /** The finger lifted in the menu along [edge] at [item], or at no item. */
    fun onEdgeMenuChoice(edge: MenuEdge, item: Int) {}
  }

  /** The trainer while it waits for attempts, or null. */
  @JvmStatic var listener: Listener? = null

  @JvmStatic fun isWaitingForAttempts(): Boolean = listener != null

  @JvmStatic
  fun isPractised(gestureId: Int): Boolean = listener?.practisedGestureId == gestureId

  @JvmStatic
  fun onTouch(event: MotionEvent) {
    listener?.onTouch(event)
  }

  @JvmStatic
  fun onTouchInteractionEnded(timeMs: Long, explored: Boolean) {
    listener?.onTouchInteractionEnded(timeMs, explored)
  }

  @JvmStatic fun onGesture(gestureId: Int): Boolean = listener?.onGesture(gestureId) ?: false

  @JvmStatic fun practiceMenuItems(edge: MenuEdge): List<String>? = listener?.practiceMenuItems(edge)

  @JvmStatic
  fun onEdgeMenuChoice(edge: MenuEdge, item: Int) {
    listener?.onEdgeMenuChoice(edge, item)
  }
}
