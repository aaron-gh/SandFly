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

import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.VisibleForTesting
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.report.ReportNotes
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Notes for problem reports on typing from the popup of alternative characters of Gboard 18.8 and
 * later, which Gboard types itself when touch exploration's hover ends: SandFly's long press of the
 * key, the end of the touch, and each text change that follows, received and kept for keyboard
 * echo. A text change received but not kept was dropped by the text event filter; none received
 * means the app or Gboard sent none. Notes name no texts, only classes and counts. (#609, #647)
 */
object GboardPopupNotes {
  private const val TAG = "GboardPopupNotes"

  /** How long after the touch ends text changes are noted. */
  @VisibleForTesting const val TEXT_CHANGE_WINDOW_MS = 3000L

  @Volatile private var touchEndTime = -1L

  /** SandFly has long-pressed a key of a Gboard that runs its popup from touch exploration. */
  @JvmStatic
  fun onLongPress(key: AccessibilityNodeInfoCompat?) {
    note("Long press of a Gboard key, class ${key?.className}")
  }

  /** The touch that long-pressed a Gboard key has ended on [lastItem]. */
  @JvmStatic
  fun onTouchEnd(lastItem: AccessibilityNodeInfoCompat?) {
    touchEndTime = SystemClock.uptimeMillis()
    note(
      "Touch ended after the long press, last item class ${lastItem?.className}, " +
        "package ${lastItem?.packageName}"
    )
  }

  /** Notes a text change [event] that SandFly received. */
  @JvmStatic
  fun onEventReceived(event: AccessibilityEvent) {
    if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return
    val after = msAfterTouchEnd(event.eventTime) ?: return
    note(
      "Text change received $after ms after the touch ended: added ${event.addedCount}, " +
        "removed ${event.removedCount}, class ${event.className}"
    )
  }

  /** Notes a text change [event] that the text event filter kept for keyboard echo. */
  @JvmStatic
  fun onTextEventKept(event: AccessibilityEvent) {
    if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return
    val after = msAfterTouchEnd(event.eventTime) ?: return
    note("Text change kept for keyboard echo, $after ms after the touch ended")
  }

  @VisibleForTesting
  internal fun msAfterTouchEnd(eventTime: Long, end: Long = touchEndTime): Long? {
    if (end < 0) return null
    val after = eventTime - end
    return if (after in -TEXT_CHANGE_WINDOW_MS..TEXT_CHANGE_WINDOW_MS) after else null
  }

  private fun note(text: String) {
    LogUtils.i(TAG, "%s", text)
    ReportNotes.gboardPopup.add(text)
  }
}
