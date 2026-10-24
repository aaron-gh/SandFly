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
package com.google.android.accessibility.talkback.recognition

import android.content.Context
import android.content.SharedPreferences
import com.google.android.accessibility.talkback.actor.gemini.AiProvider
import com.google.android.accessibility.talkback.actor.gemini.AiService
import com.google.android.accessibility.talkback.actor.gemini.local.LocalModelManager

/** What describes frames for the modes that use AI: the same as for Describe image. */
internal enum class AiBackend {
  /** The on-device model, when Use on-device AI is on and the model is downloaded. */
  ON_DEVICE,
  /** The AI service in use, other than Gemini, through its chat API. */
  SERVICE,
  /** Gemini, through the Live API. */
  GEMINI,
  /** No AI service is in use. */
  NONE;

  companion object {
    fun of(context: Context, prefs: SharedPreferences): AiBackend =
      when {
        LocalModelManager.get(context).answersRequests() -> ON_DEVICE
        AiService.isSetUp(prefs) -> SERVICE
        AiProvider.active(prefs) == AiProvider.GEMINI -> GEMINI
        else -> NONE
      }

    /** Whether the on-device model is a small one, which answers plain questions better. */
    fun usesPlainPrompts(context: Context): Boolean =
      LocalModelManager.get(context).activeModel()?.writesJson == false
  }
}
