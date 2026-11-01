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

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

/**
 * The current keyboard's speech through [SpeechControl]. The keyboard the user typed with already
 * sees everything they type, so it speaks without being allowed in Let other apps speak, and like
 * keyboard echo: without the rate limit or sound, while the device is locked, and interrupting
 * SandFly's own speech. A keyboard that says what is typed with it can also turn off SandFly's
 * echo of its typing, so it isn't said twice.
 *
 * Only the app that is the current keyboard can do either. Once the user switches keyboards, the
 * old one speaks as any other app and SandFly echoes typing again.
 */
object KeyboardSpeech {
  /** The keyboard that says what is typed with it, by package. */
  @Volatile private var typingSpokenBy: String? = null

  /** The first of [apps], the caller's packages, that is the current keyboard, or null. */
  @JvmStatic
  fun currentKeyboard(context: Context, apps: List<String>): String? =
    currentKeyboardPackage(context)?.takeIf { it in apps }

  /**
   * Records whether the current keyboard, if it is one of [apps], says what is typed with it.
   * Returns false if none of [apps] is the current keyboard.
   */
  @JvmStatic
  fun setTypingSpoken(context: Context, apps: List<String>, spoken: Boolean): Boolean {
    val keyboard = currentKeyboard(context, apps) ?: return false
    if (spoken) {
      typingSpokenBy = keyboard
    } else if (typingSpokenBy == keyboard) {
      typingSpokenBy = null
    }
    return true
  }

  /** Whether the current keyboard says what is typed with it, so SandFly doesn't echo it. */
  @JvmStatic
  fun isTypingSpoken(context: Context): Boolean {
    val keyboard = typingSpokenBy ?: return false
    return keyboard == currentKeyboardPackage(context)
  }

  /** Forgets the keyboard, for tests. */
  @JvmStatic
  fun clear() {
    typingSpokenBy = null
  }

  private fun currentKeyboardPackage(context: Context): String? =
    Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
      ?.let { ComponentName.unflattenFromString(it)?.packageName }
}
