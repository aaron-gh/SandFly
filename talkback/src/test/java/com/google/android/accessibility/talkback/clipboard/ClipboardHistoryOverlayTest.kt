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

package com.google.android.accessibility.talkback.clipboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ClipboardHistoryOverlayTest {
  private val own = "com.example.sandfly"

  @Test
  fun anotherAppsWindowClosesTheHistory() {
    assertTrue(ClipboardHistoryOverlay.closesFor("com.android.dialer", "com.example.notes", own))
    assertTrue(ClipboardHistoryOverlay.closesFor("com.android.dialer", null, own))
  }

  @Test
  fun theAppItWasOpenedOverKeepsItOpen() {
    assertFalse(ClipboardHistoryOverlay.closesFor("com.example.notes", "com.example.notes", own))
  }

  @Test
  fun sandFlysAndSystemUiWindowsAndUnknownPackagesKeepItOpen() {
    assertFalse(ClipboardHistoryOverlay.closesFor(own, "com.example.notes", own))
    assertFalse(ClipboardHistoryOverlay.closesFor(null, "com.example.notes", own))
    assertFalse(ClipboardHistoryOverlay.closesFor("", "com.example.notes", own))
    assertFalse(
      ClipboardHistoryOverlay.closesFor("com.android.systemui", "com.example.notes", own)
    )
  }
}
