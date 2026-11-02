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

package com.google.android.accessibility.talkback.actor.gemini

import android.os.Looper
import com.google.android.accessibility.talkback.actor.gemini.DataFieldUtils.GeminiResponse
import com.google.android.accessibility.talkback.actor.gemini.GeminiRestRequestPerformer.GeminiRestResponseCallback
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiServiceRequestPerformerTest {
  @Test
  fun aFailureBeforeSendingComesAfterTheCallReturns() {
    val failures = mutableListOf<GeminiFailure>()
    val callback =
      object : GeminiRestResponseCallback {
        override fun onResponse(response: GeminiResponse) {}

        override fun onFailure(failure: GeminiFailure) {
          failures += failure
        }

        override fun onCancelled() {}
      }
    AiServiceRequestPerformer(RuntimeEnvironment.getApplication())
      .performRequest("http://example.com", "model", "key", JSONObject(), callback)
    // The caller starts its progress tone here, which the failure has to stop.
    assertTrue(failures.isEmpty())
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals(1, failures.size)
  }
}
