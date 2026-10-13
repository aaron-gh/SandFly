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

package com.google.android.accessibility.talkback.speechcontrol

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Takes speech requests sent as broadcasts. Backtalk has to know which app sent each one, to ask
 * the user about it, so a broadcast is only taken from Android 14, and only when the app shares
 * its identity with BroadcastOptions.setShareIdentityEnabled. Apps that can't can bind to
 * [SpeechControlService] instead.
 */
class SpeechControlReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val sender =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) sentFromPackage else null
    if (sender == null) {
      LogUtils.i(TAG, "Ignored %s, since the app that sent it didn't share its identity", intent.action)
      return
    }
    val apps = listOf(sender)
    when (intent.action) {
      SpeechControl.ACTION_SPEAK ->
        SpeechControl.speak(
          apps,
          intent.getStringExtra(SpeechControl.EXTRA_TEXT),
          interrupt = intent.getBooleanExtra(SpeechControl.EXTRA_INTERRUPT, false),
          rate = floatExtra(intent, SpeechControl.EXTRA_RATE),
          pitch = floatExtra(intent, SpeechControl.EXTRA_PITCH),
          volume = floatExtra(intent, SpeechControl.EXTRA_VOLUME),
        )
      SpeechControl.ACTION_STOP -> SpeechControl.stop(apps)
    }
  }

  private companion object {
    const val TAG = "SpeechControlReceiver"

    fun floatExtra(intent: Intent, name: String): Float =
      when (@Suppress("DEPRECATION") val value = intent.extras?.get(name)) {
        is Number -> value.toFloat()
        is String -> value.toFloatOrNull() ?: 1f
        else -> 1f
      }
  }
}
