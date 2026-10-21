/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Lets automation apps, such as Tasker or MacroDroid, turn notification speech on or off, stop a
 * notification being read, or have the notifications read. Each action is SandFly's app ID
 * followed by [ON], [OFF], [TOGGLE], [STOP] or [READ_ALL], sent as a broadcast to SandFly.
 */
class NotificationAutomationReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    when (action.removePrefix(context.packageName)) {
      ON -> NotificationReader.setSpeechOn(context, true, announce = false)
      OFF -> NotificationReader.setSpeechOn(context, false, announce = false)
      TOGGLE -> NotificationReader.setSpeechOn(context, null, announce = false)
      STOP -> NotificationReader.stop()
      READ_ALL -> NotificationReader.readSummary()
    }
  }

  companion object {
    const val ON = ".action.NOTIFICATION_SPEECH_ON"
    const val OFF = ".action.NOTIFICATION_SPEECH_OFF"
    const val TOGGLE = ".action.NOTIFICATION_SPEECH_TOGGLE"
    const val STOP = ".action.STOP_NOTIFICATION_SPEECH"
    const val READ_ALL = ".action.READ_NOTIFICATIONS"

    val ALL = listOf(ON, OFF, TOGGLE, STOP, READ_ALL)
  }
}
