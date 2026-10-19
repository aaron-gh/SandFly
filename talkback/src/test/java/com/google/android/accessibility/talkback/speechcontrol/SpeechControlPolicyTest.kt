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

import android.text.SpannableString
import android.text.Spanned
import android.text.style.URLSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpeechControlPolicyTest {
  @Test
  fun clampScale_keepsRateAndPitchInRange() {
    assertEquals(0.25f, SpeechControlPolicy.clampScale(0f))
    assertEquals(4f, SpeechControlPolicy.clampScale(100f))
    assertEquals(1.5f, SpeechControlPolicy.clampScale(1.5f))
    assertEquals(1f, SpeechControlPolicy.clampScale(Float.NaN))
  }

  @Test
  fun clampVolume_keepsVolumeInRange() {
    assertEquals(0f, SpeechControlPolicy.clampVolume(-1f))
    assertEquals(1f, SpeechControlPolicy.clampVolume(2f))
    assertEquals(1f, SpeechControlPolicy.clampVolume(Float.NaN))
  }

  @Test
  fun textToSay_dropsNothingToSay() {
    assertNull(SpeechControlPolicy.textToSay(null))
    assertNull(SpeechControlPolicy.textToSay("   "))
  }

  @Test
  fun textToSay_dropsStyling() {
    val styled = SpannableString("Open link")
    styled.setSpan(URLSpan("https://example.com"), 0, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    val say: CharSequence? = SpeechControlPolicy.textToSay(styled)
    assertEquals("Open link", say)
    assertFalse(say is Spanned)
  }

  @Test
  fun textToSay_keepsLongTextWhole() {
    val long = "word ".repeat(10_000).trim()
    assertEquals(long, SpeechControlPolicy.textToSay(long))
  }

  @Test
  fun rateLimiter_allowsABurstThenAboutOnceASecond() {
    val limiter = SpeechControlPolicy.RateLimiter(burst = 3, refillMs = 1000)
    repeat(3) { assertTrue(limiter.tryAcquire("app", nowMs = 0)) }
    assertFalse(limiter.tryAcquire("app", nowMs = 0))
    assertFalse(limiter.tryAcquire("app", nowMs = 500))
    assertTrue(limiter.tryAcquire("app", nowMs = 1500))
    assertFalse(limiter.tryAcquire("app", nowMs = 1600))
  }

  @Test
  fun rateLimiter_keepsEachAppSeparate() {
    val limiter = SpeechControlPolicy.RateLimiter(burst = 1, refillMs = 1000)
    assertTrue(limiter.tryAcquire("one", nowMs = 0))
    assertFalse(limiter.tryAcquire("one", nowMs = 0))
    assertTrue(limiter.tryAcquire("two", nowMs = 0))
  }
}
