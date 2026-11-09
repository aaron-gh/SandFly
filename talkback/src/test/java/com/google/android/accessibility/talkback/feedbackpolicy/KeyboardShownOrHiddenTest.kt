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

package com.google.android.accessibility.talkback.feedbackpolicy

import com.google.android.accessibility.utils.input.WindowEventInterpreter.EventInterpretation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeyboardShownOrHiddenTest {
  @Test
  fun aWindowAppearing_isShown() {
    assertTrue(ScreenFeedbackManager.isKeyboardShownOrHidden(interpretation(old = -1, new = 7)))
  }

  @Test
  fun aWindowDisappearing_isHidden() {
    assertTrue(ScreenFeedbackManager.isKeyboardShownOrHidden(interpretation(old = 7, new = -1)))
  }

  @Test
  fun aWindowChangingTitleOrId_isNeither() {
    assertFalse(ScreenFeedbackManager.isKeyboardShownOrHidden(interpretation(old = 7, new = 7)))
    assertFalse(ScreenFeedbackManager.isKeyboardShownOrHidden(interpretation(old = 7, new = 9)))
  }

  private fun interpretation(old: Int, new: Int) =
    EventInterpretation().apply {
      inputMethod.setOldId(old)
      inputMethod.setId(new)
    }
}
