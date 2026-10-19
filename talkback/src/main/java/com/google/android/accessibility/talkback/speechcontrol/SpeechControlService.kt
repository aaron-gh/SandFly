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

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import fyi.quin.backtalk.speech.ISpeechControl

/**
 * Takes speech requests from apps that bind to it. Each call is checked against the app that made
 * it, which Android tells Backtalk, so it works on every version.
 */
class SpeechControlService : Service() {
  private val mainHandler = Handler(Looper.getMainLooper())

  private val binder =
    object : ISpeechControl.Stub() {
      override fun speak(
        text: String?,
        interrupt: Boolean,
        rate: Float,
        pitch: Float,
        volume: Float,
      ) {
        val apps = callingApps()
        mainHandler.post { SpeechControl.speak(apps, text, interrupt, rate, pitch, volume) }
      }

      override fun stop() {
        val apps = callingApps()
        mainHandler.post { SpeechControl.stop(apps) }
      }
    }

  override fun onBind(intent: Intent): IBinder = binder

  /** The packages of the app making the current call. Read on the binder thread, before posting. */
  private fun callingApps(): List<String> =
    packageManager.getPackagesForUid(Binder.getCallingUid())?.toList().orEmpty()
}
