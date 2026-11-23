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

package com.google.android.accessibility.brailleime

import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmitActionTest {
  @Test
  fun chatFieldsSendWithTheButton() {
    // Telegram's message field: no action and no action on enter.
    assertTrue(SubmitAction.sendsWithButton(EditorInfo.IME_FLAG_NO_ENTER_ACTION))
    assertTrue(SubmitAction.sendsWithButton(EditorInfo.IME_ACTION_SEND))
    assertTrue(SubmitAction.sendsWithButton(EditorInfo.IME_ACTION_NONE))
    assertTrue(SubmitAction.sendsWithButton(EditorInfo.IME_ACTION_UNSPECIFIED))
  }

  @Test
  fun fieldsWithTheirOwnActionDoNot() {
    assertFalse(SubmitAction.sendsWithButton(EditorInfo.IME_ACTION_SEARCH))
    assertFalse(SubmitAction.sendsWithButton(EditorInfo.IME_ACTION_GO))
    assertFalse(SubmitAction.sendsWithButton(EditorInfo.IME_ACTION_NEXT))
    assertFalse(SubmitAction.sendsWithButton(EditorInfo.IME_ACTION_DONE))
  }

  @Test
  fun noActionOnEnterWinsOverTheAction() {
    assertTrue(
      SubmitAction.sendsWithButton(EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_ENTER_ACTION)
    )
  }
}
