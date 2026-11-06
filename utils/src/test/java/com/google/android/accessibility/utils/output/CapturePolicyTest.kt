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

package com.google.android.accessibility.utils.output

import android.content.Context
import android.media.AudioAttributes
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CapturePolicyTest {
  private val prefs =
    RuntimeEnvironment.getApplication().getSharedPreferences("capture", Context.MODE_PRIVATE)

  @Test
  fun speechCanBeCapturedUntilKeptOffCasts() {
    assertEquals(AudioAttributes.ALLOW_CAPTURE_BY_ALL, CapturePolicy.policy(prefs))
    prefs.edit().putBoolean(CapturePolicy.PREF_KEEP_OFF_CASTS, true).commit()
    assertEquals(AudioAttributes.ALLOW_CAPTURE_BY_NONE, CapturePolicy.policy(prefs))
    val attributes = CapturePolicy.applyTo(AudioAttributes.Builder(), prefs).build()
    assertEquals(AudioAttributes.ALLOW_CAPTURE_BY_NONE, attributes.allowedCapturePolicy)
  }
}
