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

package com.google.android.accessibility.talkback.handwriting

import android.content.Context
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils

object HandwritingSettings {
  /**
   * How long handwriting waits after a stroke for the next one before it reads what was written,
   * in milliseconds: Typing and braille > Wait after writing.
   */
  @JvmStatic
  fun waitMs(context: Context): Long =
    SharedPreferencesUtils.getIntFromStringPref(
        SharedPreferencesUtils.getSharedPreferences(context),
        context.resources,
        R.string.pref_handwriting_wait_key,
        R.string.pref_handwriting_wait_default,
      )
      .toLong()
      .coerceIn(MIN_WAIT_MS, MAX_WAIT_MS)

  private const val MIN_WAIT_MS = 100L
  private const val MAX_WAIT_MS = 2_000L
}
