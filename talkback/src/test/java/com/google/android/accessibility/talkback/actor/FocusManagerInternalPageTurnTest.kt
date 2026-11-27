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

package com.google.android.accessibility.talkback.actor

import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TikTok draws its next video again after a two-finger swipe turns the feed's page, which loses
 * the focus moved there without speaking. Putting it back must not read the window's first item.
 */
class FocusManagerInternalPageTurnTest {
  private fun info(source: Int, muted: Boolean) =
    FocusActionInfo.builder().setSourceAction(source).setForceMuteFeedback(muted).build()

  @Test
  fun focusLostAfterASilentPageTurn_isPutBackSilently() {
    assertTrue(
      FocusManagerInternal.isSilentFromPageTurn(info(FocusActionInfo.MANUAL_SCROLL, muted = true))
    )
  }

  @Test
  fun focusLostAfterASpokenScroll_isPutBackAsBefore() {
    assertFalse(
      FocusManagerInternal.isSilentFromPageTurn(info(FocusActionInfo.MANUAL_SCROLL, muted = false))
    )
  }

  @Test
  fun focusLostAfterOtherSilentFocus_isPutBackAsBefore() {
    // Such as a text field focused again without speaking.
    assertFalse(
      FocusManagerInternal.isSilentFromPageTurn(info(FocusActionInfo.ENSURE_ON_SCREEN, muted = true))
    )
    assertFalse(
      FocusManagerInternal.isSilentFromPageTurn(
        info(FocusActionInfo.LOGICAL_NAVIGATION, muted = true)
      )
    )
  }
}
