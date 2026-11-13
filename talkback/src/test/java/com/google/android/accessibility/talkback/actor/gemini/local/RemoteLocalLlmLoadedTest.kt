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
package com.google.android.accessibility.talkback.actor.gemini.local

import com.google.android.accessibility.talkback.dictation.ModelRetention
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteLocalLlmLoadedTest {
  @Test
  fun notLoadedUntilTheServiceSaysSo() {
    assertFalse(RemoteLocalLlm.likelyLoaded(false, 0, 1_000, 60_000))
  }

  @Test
  fun loadedWhileUsedRecently() {
    assertTrue(RemoteLocalLlm.likelyLoaded(true, 10_000, 69_999, 60_000))
  }

  @Test
  fun unloadedOnceIdleForTheIdleTime() {
    assertFalse(RemoteLocalLlm.likelyLoaded(true, 10_000, 70_000, 60_000))
  }

  @Test
  fun keptUntilStoppedStaysLoaded() {
    assertTrue(
      RemoteLocalLlm.likelyLoaded(true, 0, Long.MAX_VALUE / 2, ModelRetention.UNTIL_STOPPED)
    )
  }
}
