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

package com.google.android.accessibility.talkback.speech

import android.content.Context
import android.content.SharedPreferences
import com.google.android.accessibility.talkback.R

/**
 * Repeat last spoken phrase slower: how much slower the phrase is said, set with Speech > Repeat
 * slower by, as a percentage of the rate it was said at.
 */
object SlowerRepeat {

  /** The percentages that can be chosen. */
  @JvmField val PERCENTS = intArrayOf(5, 10, 15, 20, 25, 30, 40, 50)

  /** The percentage used before one is chosen, and for a damaged setting. */
  const val PERCENT_DEFAULT = 15

  /** Returns the multiple of the rate to repeat the phrase at, such as 0.85 for 15% slower. */
  @JvmStatic
  fun rateMultiple(context: Context, prefs: SharedPreferences): Float =
    rateMultiple(prefs.getString(context.getString(R.string.pref_repeat_slower_by_key), null))

  /** Returns the multiple of the rate for the percentage saved as [percent]. */
  @JvmStatic
  fun rateMultiple(percent: String?): Float {
    val value = percent?.trim()?.toIntOrNull()?.takeIf { it in PERCENTS } ?: PERCENT_DEFAULT
    return (100 - value) / 100f
  }
}
