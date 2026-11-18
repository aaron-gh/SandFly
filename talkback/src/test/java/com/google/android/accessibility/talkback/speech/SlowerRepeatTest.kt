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
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SlowerRepeatTest {
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
  }

  @Test
  fun fifteenPercentSlowerByDefault() {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    prefs.edit().remove(context.getString(R.string.pref_repeat_slower_by_key)).commit()
    assertEquals(0.85f, SlowerRepeat.rateMultiple(context, prefs), 0.0001f)
  }

  @Test
  fun chosenPercentageSetsTheRate() {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    prefs.edit().putString(context.getString(R.string.pref_repeat_slower_by_key), "40").commit()
    assertEquals(0.6f, SlowerRepeat.rateMultiple(context, prefs), 0.0001f)
    prefs.edit().remove(context.getString(R.string.pref_repeat_slower_by_key)).commit()
  }

  @Test
  fun damagedSettingUsesTheDefault() {
    assertEquals(0.85f, SlowerRepeat.rateMultiple(null), 0.0001f)
    assertEquals(0.85f, SlowerRepeat.rateMultiple("abc"), 0.0001f)
    assertEquals(0.85f, SlowerRepeat.rateMultiple("100"), 0.0001f)
    assertEquals(0.95f, SlowerRepeat.rateMultiple(" 5 "), 0.0001f)
  }
}
